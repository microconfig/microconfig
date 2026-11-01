# Quick start

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.microconfig/microconfig-core)](https://central.sonatype.com/artifact/io.microconfig/microconfig-core)
[![Latest release](https://img.shields.io/github/v/release/microconfig/microconfig)](https://github.com/microconfig/microconfig/releases/latest)
[![Slack badge](https://img.shields.io/badge/Join-Slack-brightgreen.svg)](https://join.slack.com/t/microconfig/shared_invite/zt-dflf2m0n-wOPVfAmk5eiHPn_9Omff7Q)

**Change config once. Every service, every environment gets it right.**

Write shared settings once in Git, and Microconfig builds plain config files for every microservice in every environment:
`application.yaml`, Helm values, `logback.xml`, anything. No server, no runtime, no lock-in.

* **Shared parts, written once.** `#include monitoring, postgres` instead of copy-paste between services.
* **Values that stay in sync.** `${payment-backend@server.context}` instead of copying a value from another service.
* **A new environment in one small file.** Override only what differs, inherit the rest.
* **A diff on every build.** See what changes for each service before you deploy.
* **Broken references fail the build.** A typo in `${service@key}` stops CI, not production.
* **No new language.** Plain YAML or properties plus a few directives.

Try it in 2 minutes:

```bash
brew tap microconfig/microconfig && brew install microconfig
git clone https://github.com/microconfig/microconfig-quickstart.git && cd microconfig-quickstart
microconfig -e dev
```

Then follow the [quickstart](https://microconfig.io/quickstart.html): change one line and see every service update.
Other install options (native binaries, jar) are on the [download page](https://microconfig.io/download.html).
Real-life examples of every feature are in the [features guide](https://microconfig.io/features.html).


# Cheat sheet

## Key terms
* **Component**: a folder under `components/` with configuration files. It can be a service (`orders`) or a shared part that services include (`postgres`, `monitoring`). Includes and placeholders refer to components by name, not by path, so keep names unique.
* **Config type**: the kind of configuration, detected by the file extension: application (`*.yaml`, `*.properties`), deploy, process, Helm values, Kubernetes, secret, env. Each type of a service is built into its own result file.
* **Environment**: a file in `envs/`, for example `envs/prod.yaml`, that lists the services to build. Files like `application.prod.yaml` hold overrides for that environment.
* **Group**: a named list of components inside an environment file. Build a single group with `-g`.
* **Profile**: a set of overrides applied on demand, like `application.long.yaml` used as `#include timeout-settings[long]`. Profiles work the same way as environment overrides.
* **Placeholder**: a reference like `${component@key}`, resolved during the build. A placeholder that can't be resolved fails the build.

## Syntax

| What | Syntax | Example |
|---|---|---|
| Include other components | `#include name1, name2` | `#include monitoring, postgres` |
| Include with a profile | `#include name[profile]` | `#include timeout-settings[long]` |
| Reference a value | `${component@key}` | `${payment-backend@server.context}` |
| Reference the current component | `${this@key}` | `${this@name}` |
| Default value | `${component@key:default}` | `${this@dbHost:postgres.local}` |
| Value from another config type | `${type::component@key}` | `${app::orders@server.port}` |
| Environment variable, system property | `${env@NAME}`, `${system@name}` | `${env@BUILD_NUMBER}` |
| Expression | `#{...}` | `#{3 * 60 * 1000}` |
| Property for placeholders only | `#var key: value` | `#var dbHost: 10.10.10.10` |
| Remove an included property | `#var key:` | `#var datasource.minimum-pool-size:` |
| Override for an environment | `application.<env>.yaml` | `application.prod.yaml` |
| Template file | `mc.template.<name>: <from> -> <to>` | `mc.template.logback: ${logback-template@configDir}/logback.xml -> logback.xml` |
| Build an environment | `microconfig -e <env>` | `microconfig -e prod` |
| Build only some groups or services | `-g <groups>`, `-s <services>` | `microconfig -e dev -s orders` |


# Microconfig overview and features

Microconfig is intended to make it easy and convenient to manage configuration for microservices (or just for a large number of services) and to reuse common parts.

If your project consists of tens or hundreds of services:

* The configuration for each service should ideally be kept separate from the code.
* The configuration for different services can have common and specific parts. Also, the configuration for the same service in different environments can have common and specific parts.
* A common part for different services (or for one service in different environments) should not be copied and pasted and must be easy to reuse.
* It must be easy to understand how the resulting configuration is generated and based on which values the config placeholders are resolved.
* Some configuration properties must be dynamic, calculated using an expression language.

Microconfig is written in Java, but it's designed to be used with systems written in any language. Microconfig defines a format for configuration sources, a syntax for placeholders, includes, excludes and overrides, an expression language for dynamic properties, and an engine that transforms the config sources into plain *.yaml or *.properties files. It can also resolve placeholders in arbitrary template files and show differences between config releases.

## The difference between Microconfig and popular DevOps tools
**Compared to config servers** (like Consul, Zookeeper, Spring Cloud Config):

Config servers solve the problem of distributing configuration dynamically at runtime (for example, over HTTP endpoints), but to distribute configuration you have to store it somewhere, ideally with a history of changes and without duplication of common parts.

**Compared to Ansible**:

Ansible is a powerful but very general engine for deployment management. It doesn't provide a common and clean way to store configuration for microservices, so many teams have to invent their own solutions on top of it.

Microconfig does one thing and does it well. It provides an approach and best practices for keeping configuration for a large number of services, and an engine that builds config sources into result files.

You can use Microconfig together with any config server and deployment framework. Configuration can be built during the deployment phase and the resulting plain config files can be copied to the filesystem, where your services can access them directly (for instance, Spring Boot can read configuration from *.yaml or *.properties), or you can distribute the resulting configuration using any config server. You can also store not only application configuration but also the configuration used to run your services, and your deployment framework can read that configuration from Microconfig to start your services with the right parameters and settings.

## Where to store the configuration
It's a good practice to keep service configuration separate from code. It doesn't require you to rebuild services every time the configuration is changed and allows you to use the same service artifacts (for instance, *.jar) for all environments, because they don't contain any environment-specific configuration. The configuration can even be updated at runtime without changes to the service source code.

The best way to follow this principle is to have a dedicated repository for configuration in your favorite version control system. You can store configuration for all microservices in the same repository to make it easy to reuse a common part and be sure the common part is consistent for all your services.

## Service configuration types

It's convenient to have different kinds of configuration and keep it in different files:
* Deploy configuration (the configuration used by deployment tools that describes where/how to deploy your service, like artifact repository settings, container params).
* Process configuration (the configuration used by deployment tools to start your service with the right params, like memory limits, VM params, etc.).
* Application configuration (the configuration that your service reads after start-up and uses at runtime).
* Environment variables.
* Secret configuration (note: you should not store sensitive information, like passwords, in a VCS. In a VCS you can store references (keys) to passwords and keep the passwords themselves in secure stores (like Vault) or at least in encrypted files on environment machines).
* Library-specific templates (for instance, Dockerfile, kafka.conf, cassandra.yaml, some scripts to run before/after your service start-up, etc.).

Microconfig detects the configuration type by the config file extension. The default configuration for config types:
* `*.yaml` or `*.properties` for application configuration.
* `*.deploy` for deployment configuration.
* `*.k8s` for Kubernetes configuration (see 'Kubernetes and Helm').
* `*.proc` or `*.process` for process configuration. 
* `*.helm` for Helm values (see 'Kubernetes and Helm').
* `*.env` for environment variables.
* `*.secret` for secret configuration.
* For static files, see the 'Template files' section.

You can use all the configuration types or only some of them. You can also override the default extensions or define your own config types.

# Basic folder layout
Let's take a look at a basic folder layout that you can keep in a dedicated repository.

The config root must contain two folders:
* `components/` for the configuration of services and of the shared parts they include. Inside it, you can nest folders any way you like.
* `envs/` for environment descriptors that list which services each environment runs (see 'Environment descriptor').

Without `components/` the build stops with `Root directory must contain 'components' dir`.

For every service, you have to create a folder with a unique name (the name of the service). In the service directory, we will keep common and environment-specific configurations.

So, let's imagine we have 4 microservices: 'orders', 'payments', 'service-discovery', and 'api-gateway'. For convenience, we can group services by layers: 'infra' for infrastructure services and 'core' for our business domain services. The resulting layout will look like this:

```
repo
└───components
    └───core  
    │   └───orders
    │   └───payments
    │	
    └───infra
        └───service-discovery
        └───api-gateway
```

## Configuration sources

Inside the service folder, you can create a configuration in `key=value` format. In the following examples we will use *.yaml, but Microconfig also supports *.properties. 

Let's create the basic application and process configuration files for each service.
You can split configuration among several files, but for simplicity, we will create `application.yaml` and `process.proc` for each service. No matter how many base files are used, after the configuration build for each service and each config type, a single result file will be generated.

```
repo
└───components
    └───core  
    │    └───orders
    │    │   └───application.yaml
    │    │   └───process.proc
    │    └───payments
    │        └───application.yaml
    │        └───process.proc
    │	
    └───infra
        └───service-discovery
        │   └───application.yaml
        │   └───process.proc
        └───api-gateway
            └───application.yaml
            └───process.proc
```

In process.proc we will store the configuration that describes what your service is and how to run it (your config files can have other properties, so don't pay attention to the concrete values).

**orders/process.proc**
```properties
artifact=org.example:orders:19.4.2 # partial duplication
java.main=org.example.orders.OrdersStarter
java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # duplication
```
**payments/process.proc**
```properties
artifact=org.example:payments:19.4.2 # partial duplication
java.main=org.example.payments.PaymentStarter
java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # duplication
instance.count=2
```
**service-discovery/process.proc**
```properties
artifact=org.example.discovery:eureka:19.4.2 # partial duplication 
java.main=org.example.discovery.EurekaStarter
java.opts.mem=-Xms1024M -Xmx2048M # partial duplication 
```

As you can see, we already have some duplication (all services have version '19.4.2', and two of them have the same java.opts.mem params). Configuration duplication is as bad as code duplication. We will see later how to do it better.

Let's see what application properties look like. In the comments we note what can be improved.

**orders/application.yaml**
```yaml
server.port: 9000
application.name: orders # better to get name from folder
orders.personalRecommendation: true
statistics.enableExtendedStatistics: true
service-discovery.url: http://10.12.172.11:6781 # are you sure url is consistent with SD configuration?
eureka.instance.prefer-ip-address: true  # duplication
datasource:
  minimum-pool-size: 2  # duplication
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV  # partial duplication
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo: true  # duplication
```
**payments/application.yaml**
```yaml
server.port: 8080
application.name: payments # better to get name from folder
payments:
  bookTimeoutInMs: 900000 # difficult to read. How long in minutes?
  system.retries: 3
consistency.validateConsistencyIntervalInMs: 420000 # difficult to read. How long in minutes?
service-discovery.url: http://10.12.172.11:6781 # are you sure url is consistent with eureka configuration?
eureka.instance.prefer-ip-address: true  # duplication
datasource:
  minimum-pool-size: 2  # duplication
  maximum-pool-size: 5
datasource.url: jdbc:oracle:thin:@172.30.162.127:1521:ARMSDEV  # partial duplication
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo: true # duplication
```
**service-discovery/application.yaml**
```yaml
server.port: 6781
application.name: eureka
eureka:
  client.fetchRegistry: false
  server:
    eviction-interval-timer-in-ms: 15000 # difficult to read
    enable-self-preservation: false    
```

The first problem is that the application files contain duplication. 
Also, it takes time to understand the application's dependencies and structure. 
For instance, the payments service contains settings for:
* service-discovery client
* oracle db 
* application-specific settings 

Of course, you can separate groups of settings with empty lines. But we can make it more readable and understandable.

# Better config structure using #include
Our services have a common configuration for service-discovery and database. To make it easy to understand the service's dependencies, let's create folders for service-discovery-client and oracle-client and specify links to these dependencies from the core services.

```
repo
└───components
    └───common
    |   └───service-discovery-client 
    |   |   └───application.yaml
    |   └───oracle-client
    |       └───application.yaml
    |	
    └───core  
    │   └───orders
    │   │   ***
    │   └───payments
    │       ***
    │	
    └───infra
        └───service-discovery
        │   ***
        └───api-gateway
            ***
```
**service-discovery-client/application.yaml**
```yaml
service-discovery.url: http://10.12.172.11:6781 # are you sure url is consistent with eureka configuration?
eureka.instance.prefer-ip-address: true 
```

**oracle-client/application.yaml**
```yaml
datasource:
  minimum-pool-size: 2  
  maximum-pool-size: 5
  url: jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV  
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo: true
```

Then replace the explicit configs with includes:

**orders/application.yaml**
```yaml
#include service-discovery-client, oracle-client

server.port: 9000
application.name: orders # better to get name from folder
orders.personalRecommendation: true
statistics.enableExtendedStatistics: true
```

**payments/application.yaml**
```yaml
#include service-discovery-client, oracle-client

server.port: 8080
application.name: payments # better to get name from folder
consistency.validateConsistencyIntervalInMs: 420000 # difficult to read. How long in minutes?
payments:
  bookTimeoutInMs: 900000 # how long in minutes?
  system.retries: 3
```
To include a component's configuration, you only need to specify the component name, not its path. This makes refactoring the config layout easier. Microconfig will find a folder with the component's name and include the configuration from its files (if the folder name is not unique, Microconfig includes configs from each folder, but it's a good idea to keep component names unique).

Some problems remain, but we removed the duplication and made it easier to understand the service's dependencies.

You can override any property from your dependencies.
Let's override the connection pool size for orders.

**orders/application.yaml**
```yaml    
#include oracle-client

datasource.maximum-pool-size: 10
***
```

Nice. But the orders service now contains a small part of its db configuration (pool-size). It's not that bad, but we can make the config semantically better.
You can also see that the orders and payments services use different Oracle IPs:

orders: datasource.url: jdbc:oracle:thin:@172.30.162.<b>31</b>:1521:ARMSDEV  
payments: datasource.url: jdbc:oracle:thin:@172.30.162.<b>127</b>:1521:ARMSDEV  
And oracle-client contains settings for .31.

Of course, you can override datasource.url in payments/application.yaml. But this overridden property will contain a copy of the rest of the JDBC URL, and you will get all the standard copy-and-paste problems. We would like to override only a part of the property. 

It's also better to create dedicated configurations for the orders db and the payments db. Both will include the common db config and override the 'ip' part of the URL. After that, we will move 'datasource.maximum-pool-size' from the orders service to order-db, so the orders service will contain only links to its dependencies and service-specific configs.

Let's refactor.
```
repo
└───components
    └───common
    |   └───oracle
    |       └───oracle-common
    |       |   └───application.yaml
    |       └───order-db
    |       |   └───application.yaml
    |       └───payment-db
    |           └───application.yaml
```

**oracle-common/application.yaml**
```yaml
datasource:
  minimum-pool-size: 2  
  maximum-pool-size: 5    
connection.timeoutInMs: 300000
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo: true
```
**order-db/application.yaml**
```yaml
#include oracle-common
    
datasource:
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV #partial duplication
```
**payment-db/application.yaml**
```yaml
#include oracle-common
    
datasource.url: jdbc:oracle:thin:@172.30.162.127:1521:ARMSDEV #partial duplication
```

**orders/application.yaml**
```yaml
#include order-db
***
```

**payments/application.yaml**
```yaml
#include payment-db
```

Includes can be on one line or on separate lines:

```yaml
#include service-discovery-client
#include oracle-client, monitoring    
```

# Placeholders

Instead of duplicating the value of a property, Microconfig lets you link to it with a placeholder. 

Let's refactor the service-discovery-client config.

Initial:

**service-discovery-client/application.yaml**
```yaml
service-discovery.url: http://10.12.172.11:6781 # are you sure host and port are consistent with SD configuration? 
```
**service-discovery/application.yaml**
```yaml
server.port: 6781 
```

Refactored:

**service-discovery-client/application.yaml**
```yaml
service-discovery.url: http://${service-discovery@ip}:${service-discovery@server.port}
```
**service-discovery/application.yaml**
```yaml
server.port: 6781
ip: 10.12.172.11 
```
So if you change the service-discovery port, all dependent services will get this update.

Microconfig has another way to store a service's IP. We will discuss it later. For now, it's better to set the 'ip' property in the service-discovery config file. 

The Microconfig syntax for placeholders: `${componentName@propertyName}`. Microconfig forces you to specify the component name. This syntax is better than just a property name
(like `${connectionSize}`) because it makes it obvious where to find the original placeholder value.

Let's refactor the Oracle db config using placeholders and environment-specific overrides.

Initial:

**oracle-common/application.yaml**
```yaml    
datasource:
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV 
```   

Refactored:

**oracle-common/application.yaml**
```yaml
datasource:
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@${this@oracle.host}:1521:${this@oracle.sid}
oracle:
  host: 172.30.162.20
  sid: ARMSDEV
```
**oracle-common/application.uat.yaml**
```yaml
oracle.host: 172.30.162.80
```

**oracle-common/application.prod.yaml**
```yaml
oracle:
  host: 10.17.14.18
  sid: ARMSPROD
```

As you can see, with placeholders we can override not only a whole property but also a part of it.

A placeholder can link to another placeholder. Microconfig can resolve them recursively and detect cyclic dependencies.

## Temp properties

If you want to declare temp properties that will be used by placeholders only and you don't want them to be included in the result config file, you can declare them with the `#var` keyword.

**oracle-common/application.yaml**
```yaml
datasource.url: jdbc:oracle:thin:@${this@oracle.host}:1521:${this@oracle.sid}
#var oracle.host: 172.30.162.20
#var oracle.sid: ARMSDEV
```
**oracle-common/application.uat.yaml**
```yaml
#var oracle.host: 172.30.162.80
``` 

This approach works with includes as well. You can #include oracle-common and then override 'oracle.host', and 'datasource.url' will be resolved based on the overridden value.

In the example below, after the build, datasource.url will be: jdbc:oracle:thin:@**100.30.162.80**:1521:ARMSDEV

 
**order-db/application.dev.yaml** 
```yaml   
#include oracle-common
 
#var oracle.host: 100.30.162.80         
```
## Removing base properties
With `#var` you can also remove properties from the result config file: include a config and redeclare any of its properties with `#var` to exclude it from the result.

Let's remove the 'datasource.minimum-pool-size' property from the payments config:

**db-client/application.yaml**
```yaml
datasource:
  minimum-pool-size: 2  
  maximum-pool-size: 5
```
**payments/application.yaml**
```yaml
#include db-client
# the empty #var below removes the property from the result config
#var datasource.minimum-pool-size:
```

## Placeholder's default value
You can specify a default value for a placeholder using the following syntax: ${component@property:**defaultValue**}

Let's set a default value for the Oracle host:

**oracle-common/application.yaml**
```yaml
datasource:
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@${this@oracle.host:172.30.162.20}:1521:${this@oracle.sid}
#var oracle.sid: ARMSDEV
```
Note that a default value can itself be a placeholder:
 `${component@property:${component2@property7:Missing value}}`
 
In this example, Microconfig will try to:
* resolve `${component@property}`
* if the above is missing - resolve `${component2@property7}`
* if the above is missing - return 'Missing value'

If a placeholder doesn't have a default value and that placeholder can't be resolved, Microconfig throws an exception with a detailed description of the problem.

## Special placeholders
As we discussed, the syntax for placeholders looks like `${component@property}`.
Microconfig has several useful special placeholders:

* `${this@env}` - returns the current environment name.
* `${...@name}` - returns the component's name.
* `${...@configDir}` - returns the full path of the component's config dir.
* `${...@resultDir}` - returns the full path of the component's destination dir (the resulting files will be put into this dir).
* `${this@configRoot}` - returns the full path of the config repository root dir (see the `-r` build param).

There are some other properties related to the environment descriptor; we will discuss them later:
* `${...@ip}`
* `${...@portOffset}`
* `${...@group}`
* `${...@order}`

Note that if you use a special placeholder with `${this@...}`, the value will be context-dependent. Let's apply `${this@name}` to see why it's useful.

Initial:

**orders/application.yaml**
```yaml
#include service-discovery-client

application.name: orders
```
**payments/application.yaml**
```yaml
#include service-discovery-client

application.name: payments
```

Refactored:

**orders/application.yaml**
```yaml
#include service-discovery-client
```
**payments/application.yaml**
```yaml
#include service-discovery-client
```
**service-discovery-client/application.yaml**
```yaml
application.name: ${this@name}
``` 

## Environment variables and system properties 
To resolve environment variables, use the following syntax: `${env@variableName}`

For example:
```
 ${env@Path}
 ${env@JAVA_HOME}
 ${env@NUMBER_OF_PROCESSORS}
```

To resolve Java system properties (System::getProperty), use the following syntax: `${system@variableName}`

Some useful standard system properties:

```
 ${system@user.home}
 ${system@user.name}
 ${system@os.name}
```

You can pass your own system properties when starting Microconfig with the `-D` prefix (see the 'Running the config build' section).

Example:
```
 -DtaskId=3456 -DsomeParam3=value
```
Then you can access them: `${system@taskId}` or `${system@someParam3}`

## Placeholders to another config type

As we discussed, Microconfig supports different config types and detects the type by file extension. Microconfig resolves placeholders based on properties of the **same config type** only.

Let's see how it works:

'orders' has the 'server.port' property in application.**yaml**, so you can declare a placeholder to this property only from application config types (*.yaml or *.properties). If you declare that placeholder in, for example, a *.process file, Microconfig will fail to resolve it and throw an exception.

**someComponent/application.yaml**
```yaml
orderPort: ${orders@server.port} # works
```
**someComponent/application.process**
```yaml
orderPort: ${orders@server.port} # doesn’t work
```

If you need to declare a placeholder to a property from another config type, you have to specify the config type using the following syntax: ${**configType**::component@property}.

For our example, the correct syntax is:

**someComponent/application.process**
```yaml
orderPort: ${app::orders@server.port}
```

Microconfig default config types:
* `app` – for *.yaml or *.properties
* `process` – for *.proc or *.process
* `deploy` – for *.deploy
* `secret` – for *.secret
* `env` – for *.env

# Expression language
Microconfig supports math expressions, conditions and the Java [String API](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html).

Let's see some examples:

```yaml
#Better than 300000
connection.timeoutInMs: #{5 * 60 * 1000}

#Microconfig placeholder and simple math
datasource.maximum-pool-size: #{${this@datasource.minimum-pool-size} + 10} 

#Using placeholder and Java String API
mainClassNameWithoutPackage: #{'${this@java.main}'.substring('${this@java.main}'.lastIndexOf('.') + 1)}

month: #{'${this@yyyy-MM-dd}'.split('-')[1]}

#Condition
releaseType: #{'${this@version}'.endsWith('-SNAPSHOT') ? 'snapshot' : 'release'}

```

# Environment specific properties
Microconfig allows specifying environment-specific properties (add/remove/override). For instance, you may want to increase the connection pool size for databases and the amount of memory in the prod environment.
To add/remove/override properties for an environment, create an application.**${ENVNAME}**.yaml file in the config folder. 

Let's override the connection pool size for 'dev' and 'prod' and add one new param for 'dev'. 

```
order-db
└───application.yaml
└───application.dev.yaml
└───application.prod.yaml
```

**order-db/application.dev.yaml**
```yaml   
datasource.maximum-pool-size: 15   
hibernate.show-sql: true
```

**order-db/application.prod.yaml**
```yaml   
datasource.maximum-pool-size: 50
```

You can also declare common properties for several environments in a single file. You can use the following filename pattern: application.**${ENV1.ENV2.ENV3...}**.yaml

Let's create common properties for dev, dev2 and test environments.

```
order-db
└───application.yaml
└───application.dev.yaml
└───application.dev.dev2.test.yaml
└───application.prod.yaml
```

**order-db/application.dev.dev2.test.yaml**
```yaml   
hibernate.show-sql: true
```

When you build configs for a specific environment (for example, 'dev'), Microconfig will collect properties from:
* application.yaml 
* then add/override properties from application.dev.{anotherEnv}.yaml.
* then add/override properties from application.dev.yaml.
 
## Profiles and explicit environment names for includes and placeholders
As we discussed, you can create environment-specific properties using the filename pattern application.${ENV}.yaml. You can use the same approach to create profile-specific properties.

For example, you can create a folder for http client timeout settings:

**timeout-settings/application.yaml**
```yaml
timeouts:
  connectTimeoutMs: 1000
  readTimeoutMs: 5000
```
And some services can include this configuration:

**orders/application.yaml**
```yaml
#include timeout-settings
```
**payments/application.yaml**
```yaml
#include timeout-settings
```

But what if you want some services to be configured with a long timeout? Instead of an environment name, you can use a profile name in the filename:
```
timeout-settings
└───application.yaml
└───application.long.yaml
└───application.huge.yaml
```
**timeout-settings/application.long.yaml**
```yaml
timeouts.readTimeoutMs: 30000
```
**timeout-settings/application.huge.yaml**
```yaml
timeouts.readTimeoutMs: 600000
```

Then specify the profile name in the include:

**payments/application.yaml**
```yaml
#include timeout-settings[long]
```

You can use the profile/environment name with placeholders as well:

```
${timeout-settings[long]@readTimeoutMs}
${kafka[test]@bootstrap-servers}
```

The difference between environment-specific files and profiles is purely logical: Microconfig handles them the same way.

# Template files
Microconfig allows you to keep configuration files for any libraries in their specific format and resolve placeholders inside them.
For example, you may want to keep logback.xml (or some other descriptor for your logging library) and reuse this file, with placeholders resolved, for all your services. 

Let's create this file:
```
repo
└───components
    └───common
    |    └───logback-template 
    |         └───logback.xml
```
**logback-template/logback.xml**
```xml
<configuration>
    <appender class="ch.qos.logback.core.FileAppender">
        <file>logs/${this@application.name}.log</file>
            <encoder>
                <pattern>%d [%thread] %highlight(%-5level) %cyan(%logger{15}) %msg %n</pattern>
            </encoder>
    </appender>    
</configuration>
```
So we want every service to have its own logback.xml with `${this@application.name}` resolved. 
Let's configure the orders and payments services to use this template.

**orders/application.yaml**
```yaml
#include service-discovery-client

mc.template.logback.fromFile: ${logback-template@configDir}/logback.xml # full path to logback.xml, @configDir - special placeholder property
```

**payments/application.yaml**
```yaml
#include service-discovery-client

mc.template.logback.fromFile: ${logback-template@configDir}/logback.xml
```  
   
It's better to extract the common property `mc.template.logback.fromFile` to logback-template/application.yaml and then use #include.

```
repo
└───components
    └───common
    |   └───logback-template 
    |       └───logback.xml
    |       └───application.yaml
```    
**logback-template/application.yaml**
```yaml   
mc.template.logback.fromFile: ${logback-template@configDir}/logback.xml
```
**orders/application.yaml**
```yaml
#include service-discovery-client, logback-template
```
**payments/application.yaml**
```yaml
#include service-discovery-client, logback-template
```  

As we saw above, the orders and payments services include the `application.name` property from service-discovery-client.
During the config build Microconfig will replace `${this@application.name}` inside logback.xml with the service's property value and copy the resulting file 'logback.xml' to the relevant folder for each service.

If you want to declare a property for a template only and don't want this property to be included in the result config file, you can use the `#var` keyword. 

If you want to override the template destination filename, you can use the `mc.template.${templateName}.toFile=${someFile}` property. For example:  
 
 **logback-template/application.yaml**
 ```yaml   
 mc.template.logback:
   fromFile: ${logback-template@configDir}/logback.xml
   toFile: logs/logback-descriptor.xml
 ``` 
 
You can use an absolute or a relative path for the `toFile` property. A relative path starts from the service's result config dir (see the 'Running the config build' section).

So the template dependency declaration syntax looks like:   
```
mc.template.${templateName}:
  fromFile: ${sourceTemplateFile}
  toFile: ${resolvedTemplateDestinationFile}
```
`${templateName}` is used only for mapping the `fromFile` and `toFile` properties.

You can also use a short one-line syntax with `->`:
 ```
 mc.template.${templateName}: ${sourceTemplateFile} -> ${resolvedTemplateDestinationFile}
 ```
With the arrow notation, you can also resolve whole directories of templates. This resolves all files inside `${sourceTemplateDir}`, and each source filename is used as the target filename.
 ```
 mc.template.${templateName}: ${sourceTemplateDir}/* -> ${resolvedTemplateDestinationDir}
 ```
Or create multiple templates with different `${templateName}` from a single template file. 
Note the `${templateName}` placeholder inside the target filename: it creates a separate file for each template name.
 ```
 mc.template.[${templateName1},${templateName2}]: ${sourceTemplateFile} -> ${resolvedTemplateDestinationDir}/targetName-${templateName}.xml
 ```

Let's override the file that will be copied for the prod environment:
```
repo
└───components
    └───common
    |   └───logback-template 
    |       └───logback.xml
    |       └───logback-prod.xml
    |       └───application.yaml
    |       └───application.prod.yaml
```
**logback-template/application.prod.yaml**
```yaml   
mc.template.logback.fromFile: ${logback-template@configDir}/logback-prod.xml
``` 
## Mustache template engine support
If resolving placeholders inside templates is not enough for you, you can use [Mustache template engine](https://mustache.github.io/mustache.5.html). With Mustache you can use loops, conditions and includes.

Let's imagine we want to configure different Logback appenders in different environments. We can use the condition 'appender.rolling' and override its value per environment.

```
{{#appender.rolling}}
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${this@name}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/${this@name}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>20MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
{{/appender.rolling}}
{{^appender.rolling}}
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/${this@name}.log</file>
    </appender>
{{/appender.rolling}}
```

 **logback-template/application.yaml**
 ```yaml   
mc.mustache.logback.fromFile: ${logback-template@configDir}/logback.xml
#var appender.rolling: true
 ``` 

 **logback-template/application.test.yaml**
 ```yaml   
#var appender.rolling: false
 ``` 

Microconfig uses Mustache if the template declaration starts with the `mc.mustache` prefix:
```
mc.mustache.logback.fromFile: ${logback-template@configDir}/logback.xml
```
or the template file has the `*.mustache` extension:  
``` 
mc.template.logback:
  fromFile: ${logback-template@configDir}/logback.mustache
  toFile: logger.xml
```
or the short version
``` 
mc.template.logback: ${logback-template@configDir}/logback.mustache -> logger.xml
```
## Copy binary files
To copy files as they are, without resolving placeholders, use the `mc.file.` prefix:
``` 
mc.file.someBinary: ${this@configDir}/cert.jks -> cert.jks
``` 
or
```
mc.file.someBinary:
  fromFile: ${this@configDir}/cert.jks
  toFile: cert.jks
```

# Kubernetes and Helm
Microconfig doesn't replace Helm or kubectl. It builds the values and configs they consume, with the same includes, placeholders and environment overrides as application configs.

Keep Helm values in `*.helm` files and Kubernetes settings in `*.k8s` files next to the service's application config:
```
repo
└───components
    └───orders
        └───application.yaml
        └───values.helm
        └───values.prod.helm
        └───deployment.k8s
```
**orders/values.helm**
```yaml
replicaCount: 2
image:
  repository: example/${this@name}
  tag: "1.5"
service:
  port: ${app::this@server.port}
```
**orders/values.prod.helm**
```yaml
replicaCount: 3
```
`${app::this@server.port}` reads the port from the service's application config, so the chart and the application can't disagree on it (see 'Placeholders to another config type').

`microconfig -e prod` builds:
```
build
└───orders
    └───application.yaml
    └───values.yaml
    └───k8s.yaml
```
**build/orders/values.yaml**
```yaml
replicaCount: 3

image:
  repository: example/orders
  tag: "1.5"

service:
  port: 9000
```
The result files are named after the config type: all `*.helm` files of a service are merged into `values.yaml`, and all `*.k8s` files into `k8s.yaml`. Pass them to your tools as usual, for example `helm upgrade --install orders ./chart -f build/orders/values.yaml`.

Don't put secret values into these files: everything in them ends up in the build output. Keep only references, such as the name of a Kubernetes Secret, and let the cluster inject the values.

# Environment descriptor
As we discussed, every service can have default and environment-specific configurations, and common configuration can be extracted into separate components. 
During the build phase we want to build configs for a subset of our components: only for real services in a specific environment.
Of course, you can pass the environment name and the list of service names as parameters to build the configuration. But this is not very convenient if you want to build configuration for a large number of services.

So Microconfig lets you list service names in a special environment descriptor and then use only the environment name to build configs for all services listed in it.

Environment descriptors must be in the `${configRoot}/envs` folder.
``` 
repo
└───components
|   └───***   
└───envs
    └───base.yaml
    └───dev.yaml
    └───test.yaml
    └───prod.yaml
```

Let's see the environment descriptor format:
 
 **envs/base.yaml**
```yaml
orders:  
  components:  
    - order-db-patcher
    - orders
    - order-ui

payments:
  components:
    - payment-db-patcher
    - payments
    - payment-ui

infra:
  components: 
    - service-discovery
    - api-gateway
    - ssl-api-gateway
    
monitoring:
  components:
    - grafana
    - prometheus    
```  

environment name = filename
```yaml
orders: # component group name
  components:  
    - order-db-patcher # component name(folder)
    - orders # component name
    - order-ui # component name
``` 

One environment can include another one and add/remove/override component groups:

**envs/test.yaml**
```yaml
include: # include all groups from 'base' environment except 'monitoring'
  env: base
  exclude:
   - monitoring

infra:
  exclude:
    - ssl-api-gateway # excluded ssl-api-gateway component from 'infra' group  
  append:
    - local-proxy # added new component into 'infra' group

tests_dashboard: # added new component group 'tests_dashboard'
  components:
    - test-statistic-collector
``` 

You can set the optional `ip` param for an environment or for component groups and then use it via `${componentName@ip}`.

For instance, `${orders@ip}` will be resolved to 12.53.12.67, `${payment-ui@ip}` will be resolved to 170.53.12.80.   
```yaml
ip: 170.53.12.80 # default ip

orders:  
  ip: 12.53.12.67 # ip overridden for the group
  components:  
    - order-db-patcher
    - orders
    - order-ui

payments:  
  components:
    - payment-db-patcher
    - payments
    - payment-ui    
```

Consider configuring your deployment tool to read the environment descriptor to know which services to deploy.

## Environment profiles
You can use env profiles if you want to create a new env based on one or more other envs.
For example, you have the `prod` env with its overrides and want to create `prod-europe` and `prod-usa` envs that include all properties from `prod` and can have their own overrides.
The easiest way to do this is to define the `prod` profile in `prod-europe` and `prod-usa` env descriptors:

**envs/prod.yaml**
```yaml
core:
  components:
   - order-service
   - payment-service
  ...
```
**envs/prod-europe.yaml**
```yaml
include:
  env: prod # include all components from `prod` env

profiles: 
  - prod #include all configuration from prod envs
 ```

**envs/prod-usa.yaml**
```yaml
include:
  env: prod # include all components from `prod` env

profiles: 
  - prod #include all configuration from prod envs
  - usa #include overrides from `usa` profiles (app.usa.yaml)
 ```

The config override priority for all components from `prod-usa` env:
* app.yaml `#without env`
* app.prod.yaml `#overrides for prod env`
* app.usa.yaml `#overrides for usa profile`
* app.prod-usa.yaml `#overrides for prod-usa env`

# Running the config build
As we discussed, Microconfig has its own format for configuration sources. 
During the config build, Microconfig inlines all includes, resolves placeholders, evaluates expressions, copies templates, and writes the results to plain *.yaml or *.properties files in a dedicated folder for each service.

To run the build, you can download a Microconfig release from https://github.com/microconfig/microconfig/releases.

The required build params:
* `-r` - full or relative config root dir. 
* either `-e` or `-envs`.
    * `-e` - environment name (environment is used as a config profile, also as a group of services to build configs).
    * `-envs` - a comma-separated list of environment names **without spaces**. Use `*` for all environments, and `!` to exclude an environment. E.g. `-envs *,!base`.  
`Tip: If you use zsh, be sure to escape the ! in your exclusions -> e.g. -envs *,\!base`

Optional build params:
* `-d` - full or relative build destination dir. Default = ${currentFolder}/build
* `-stacktrace` - Show full stacktrace in case of exceptions. Values: true/false. Default: false  

To build configs not for the whole environment but only for specific services, use the following optional params:
* `-g` - a comma-separated list of component groups to build configs. 
* `-s` - a comma-separated list of services to build configs. 

Command line params example (Java 17+ required):
```
java -jar microconfig.jar -r repo -e prod
```

To add system properties, use `-D`:
```
java -DtaskId=3456 -DsomeParam=value -jar microconfig.jar -r repo -d configs -e prod
```

To speed up the build by up to 3 times, add the `-XX:TieredStopAtLevel=1` JVM param. 
The native binaries (macOS, Linux, Windows) don't need `-XX:TieredStopAtLevel`: they work about 5 times faster than the jar, because no time is spent compiling code at runtime. Peak performance is the same.
Either way, even big projects with hundreds of services build in about 1-3 seconds.
```
java -XX:TieredStopAtLevel=1 -jar microconfig.jar -r repo -e prod
```

Let's see examples of initial and destination folder layouts: 

Initial source layout:
```
repo
└───components
    └───common
    |   └───logback-template 
    |       └───logback.xml
    └───core  
    │   └───orders
    │   │   └───application.yaml
    │   │   └───process.proc
    │   └───payments
    │       └───application.yaml
    │       └───process.proc
    │	
    └───infra
        └───service-discovery
        │   └───application.yaml
        │   └───process.proc
        └───api-gateway
            └───application.yaml
            └───process.proc
```
After the build:
```
configs
└───orders
│   └───application.yaml
│   └───process.yaml
|   └───logback.xml
└───payments
│   └───application.yaml
│   └───process.yaml
|   └───logback.xml
└───service-discovery
│   └───application.yaml
│   └───process.yaml
|   └───logback.xml
└───api-gateway
    └───application.yaml
    └───process.yaml
    └───logback.xml
```

You can try building configs with the example repo: https://github.com/microconfig/microconfig-quickstart 

## Viewing differences between config versions 
During the config build, Microconfig compares newly generated files with the files generated during the previous build, for each service and config type.
Microconfig can detect added/removed/changed properties. 

Diff for application.yaml is stored in diff-application.yaml, diff for process.yaml is stored in diff-process.yaml, etc.
```
configs
└───orders
│   └───application.yaml
│   └───diff-application.yaml
│   └───process.yaml
│   └───diff-process.yaml
│   └───logback.xml
```

The diff format:

**diff-application.yaml**
```yaml     
+security.client.protocol: SSL # property has been added
-connection.timeoutMs: 1000 # property has been removed
 server.max-threads: 10 -> 35 # value has been changed from '10' to '35'
```

# YAML and Properties format support
Microconfig supports the *.yaml and *.properties formats for source and result configs.
You can keep part of the configuration in *.yaml files and another part in *.properties.

```
repo
└───components
    └───core  
    │   └───orders
    │   │   └───application.yaml
    │   │   └───process.proc
    │   └───payments
    │       └───application.properties
    │       └───process.proc
```

YAML configs can have nested properties:
```yaml
datasource:  
  minimum-pool-size: 2  
  maximum-pool-size: 5    
  timeout:
    ms: 10
```      

and lists:
```yaml
cluster.gateway:
  hosts:
    - 10.20.30.47
    - 15.20.30.47
    
```

YAML configs are built into *.yaml, and properties configs into *.properties. If *.properties configs include *.yaml configs, the resulting file will be *.yaml.
If a config file has neither the *.yaml nor the *.properties extension, Microconfig detects the format by the key-value separator: with `:` the file is treated as *.yaml, with `=` as *.properties.

# IntelliJ IDEA plugin
To make configuration management a little easier, you can use the Microconfig IntelliJ IDEA plugin. The plugin can navigate to the sources of #include and placeholders, show hints with resolved placeholders, and build configs from the IDE.

See the documentation here: https://github.com/microconfig/microconfig-idea-plugin

# Contributing
If you want to contribute to the project and make it better, your help is very welcome. You can request a feature or report a bug via issues, or submit a pull request.

Contributing to the documentation is very welcome too.

Your Github 'Star' is appreciated!

https://github.com/microconfig/microconfig

# Contacts
[Join our Slack!](https://join.slack.com/t/microconfig/shared_invite/zt-dflf2m0n-wOPVfAmk5eiHPn_9Omff7Q)

support@microconfig.io
