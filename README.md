# Microconfig overview and features

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.microconfig/microconfig-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.microconfig/microconfig-core)
[![Build Status](https://travis-ci.com/microconfig/microconfig.svg?branch=master)](https://travis-ci.com/microconfig/microconfig)
[![Slack badge](https://img.shields.io/badge/Join-Slack-brightgreen.svg)](https://join.slack.com/t/microconfig/shared_invite/zt-dflf2m0n-wOPVfAmk5eiHPn_9Omff7Q)

Microconfig is intended to make it easy and convenient to manage configuration for microservices (or just for a big amount of services) and reuse the common part.

If your project consists of tens or hundreds of services you have to:

* Keep the configuration for each service ideally separate from the code.
* The configuration for different services can have common and specific parts. Also, the configuration for the same service in different environments can have common and specific parts.
* A common part for different services (or for one service in different environments) should not be copied and pasted and must be easy to reuse.
* It must be easy to understand how the resulting configuration is generated and based on which values the config placeholders are resolved.
* Some configuration properties must be dynamic, calculated using an expression language.

Microconfig is written in Java, but it's designed to be used with systems written in any language. Microconfig just describes a format of configuration sources, syntax for placeholders, includes, excludes, overrides, an expression language for dynamic properties and an engine that can transform the config sources into simple *.yaml or *.properties files. Also it can resolve placeholders in arbitrary template files and show differences between config releases.

## The difference between Microconfig and popular DevOps tools
**Compared to config servers** (like Consul, Zookeeper, Spring Cloud Config):

Config servers solve the problem of dynamic distribution of configuration in runtime (can use http endpoints), but to distribute configuration you have to store it, ideally with changes in history and without duplication of common parts.

**Compared to Ansible**:

Ansible is a powerful, but much too general, engine for deployment management and doesn't provide a common and clean way to store configuration for microservices, and a lot of teams have to invent their own solutions based on Ansible.

Microconfig does one thing and does it well. It provides an approach, best practices for how to keep configuration for a big amount of services, and an engine to build config sources into result files.

You can use Microconfig together with any config servers and deployment frameworks. Configuration can be built during the deployment phase and the resulting plain config files can be copied to the filesystem, where your services can access them directly (for instance, Spring Boot can read configuration from *.yaml or *.properties), or you can distribute the resulting configuration using any config servers. Also, you can store not only application configuration but configuration used to run your services, and your deployment framework can read that configuration from Microconfig to start your services with the right parameters and settings.

## Where to store the configuration
It’s a good practice to keep service configuration separate from code. It doesn't require you to rebuild services every time the configuration is changed and allows you to use the same service artefacts (for instance, *.jar) for all environments because it doesn’t contain any environment specific configuration. The configuration can be updated even in runtime without service source code changes.

The best way to follow this principle is to have a dedicated repository for configuration in your favorite version control system.  You can store configuration for all microservices in the same repository to make it easy to reuse a common part and be sure the common part is consistent for all your services.

## Service configuration types

It's convenient to have different kinds of configuration and keep it in different files:
* Deploy configuration (the configuration used by deployment tools that describes where/how to deploy your service, like artifact repository settings, container params).
* Process configuration (the configuration used by deployment tools to start your service with right params, like memory limits, VM params, etc.).
* Application configuration (the configuration that your service reads after start-up and uses in runtime).
* Environment variables.
* Secret configuration (note, you should not store in a VCS any sensitive information, like passwords. In a VCS you can store references(keys) to passwords, and keep passwords in special secured stores(like Vault) or at least in encrypted files on environment machines).
* Library specific templates (for instance, Dockerfile, kafka.conf, cassandra.yaml, some scripts to run before/after your service start-up, etc.)

Microconfig detects the configuration type by the config file extension. The default configuration for config types:
* `*.yaml` or `*.properties` for the application configuration.
* `*.proc` or `*.process` for the process configuration.
* `*.deploy` for the deployment configuration.
* `*.env` for environment variables.
* `*.secret` for the secret configuration.
* For static files - see the 'Templates files' section.

You can use all the configuration types or only some of them. Also you can override the default extensions or define your own config types.

# Basic folder layout
Let’s take a look at a basic folder layout that you can keep in a dedicated repository.

For every service, you have to create a folder with a unique name(the name of the service). In the service directory, we will keep common and environment specific configurations.

So, let’s imagine we have 4 microservices: 'order-service', 'payment-service', 'service-discovery', and 'api-gateway'. For convenience, we can group services by layers: 'infra' for infrastructure services and 'core' for our business domain services. The resulting layout will look like:

```
repo
└───core  
│   └───orders
│   └───payments
│	
└───infra
    └───service-discovery
    └───api-gateway
```

## Configuration sources

Inside the service folder, you can create a configuration in `key=value` format. For the following examples, we will prefer using *.yaml, but Microconfig also supports *.properties. 

Let’s create the basic application and process configuration files for each service.
You can split configuration among several files, but for simplicity, we will create `application.yaml` and `process.proc` for each service. No matter how many base files are used, after the configuration build for each service and each config type, a single result file will be generated.

```
repo
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

Inside process.proc we will store configuration that describes what your service is, and how to run it (your config files can have other properties, so don't pay attention to concrete values).

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

As you can see we already have some small duplication (all services have '19.4.2' version, and two of them have the same java.ops params).  Configuration duplication is as bad as code duplication. We will see later how to do this in a better way.

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

The first bad thing - application files contain duplication. 
Also, you have to spend some time to understand the application’s dependencies or its structure. 
For instance, payment-service contains settings for:
* service-discovery client
* oracle db 
* application specific 

Of course, you can separate a group of settings by an empty line. But we can make it more readable and understandable.

# Better config structure using #include
Our services have a common configuration for service-discovery and database. To make it easy to understand the service's dependencies, let’s create folders for service-discovery-client and oracle-client and specify links to these dependencies from the core services.

```
repo
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

And replace explicit configs with includes

**orders/application.yaml**
```yaml
#include service-discovery-client
#include oracle-db-client

server.port: 9000
application.name: orders # better to get name from folder
orders.personalRecommendation: true
statistics.enableExtendedStatistics: true
```

**payments/application.yaml**
```yaml
#include service-discovery-client
#include oracle-db-client

server.port: 8080
application.name: payments # better to get name from folder
consistency.validateConsistencyIntervalInMs: 420000 # difficult to read. How long in minutes?
payments:
  bookTimeoutInMs: 900000 # how long in minutes?
  system.retries: 3
```
To include a component's configuration you need to specify only the component name, you don't need to specify its path. This makes the config layout refactoring easier. Microconfig will find a folder with the component's name and include the configuration from its files (if the folder name is not unique, Microconfig includes configs from each folder, but it's a good idea to keep a component name unique).

Some problems still here, but we removed the duplication and made it easier to understand the service's dependencies.

You can override any properties from your dependencies.
Let's override the order's connection pool size.

**orders/application.yaml**
```yaml    
#include oracle-db-client

datasource.maximum-pool-size: 10
***
```

Nice. But order-service has a small part of its db configuration(pool-size), it's not that bad, but we can make the config semantically better.
Also you can see that order and payment services have a different ip for oracle.

order: datasource.url: jdbc:oracle:thin:@172.30.162.<b>31</b>:1521:ARMSDEV  
payment: datasource.url: jdbc:oracle:thin:@172.30.162.<b>127</b>:1521:ARMSDEV  
And oracle-client contains settings for .31.

Of course, you can override datasource.url in the payment/application.yaml. But this overridden property will contain a copy of another part of jdbc url and you will get all the standard 'copy-and-paste' problems. We would like to override only a part of the property. 

Also it's better to create a dedicated configuration for order db and payment db. Both db configuration will include common-db config and override the 'ip' part of url. After that, we will migrate 'datasource.maximum-pool-size' from order-service to order-db, so order-service will contain only links to its dependencies and service-specific configs.

Let’s refactor.
```
repo
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
**orders-db/application.yaml**
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

Also includes can be in one line:

**payments/application.yaml**
```yaml
#include service-discovery-client, oracle-db-client    
```

# Placeholders

Instead of duplicating the value of some properties, Microconfig allows you to have a link (placeholder) to this value. 

Let's refactor the service-discovery-client config.

Initial:

**service-discovery-client/application.yaml**
```yaml
service-discovery.url: http://10.12.172.11:6781 # are you sure host and port are consistent with SD configuration? 
```
**service-discovery/application.yaml**
```yaml
server.port: 6761 
```

Refactored:

**service-discovery-client/application.yaml**
```yaml
service-discovery.url: http://${service-discovery@ip}:${service-discovery@server.port}
```
**service-discovery/application.yaml**
```yaml
server.port: 6761
ip: 10.12.172.11 
```
So if you change the service-discovery port, all dependent services will get this update.

Microconfig has another approach to store service's ip. We will discuss it later. For now, it's better to set the 'ip' property in the service-discovery config file. 

The Microconfig syntax for placeholders: `${componentName@propertyName}`. Microconfig forces you to specify the component name. This syntax is better than just a property name
(like `${connectionSize}`), because it makes it obvious where to find the original placeholder value.

Let's refactor oracle db config using placeholders and environment specific overrides.

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

As you can see using placeholders we can override not only the whole property but also part of it.

A placeholder can link to another placeholder. Microconfig can resolve them recursively and detect cyclic dependencies.

## Temp properties

If you want to declare temp properties that will be used by placeholders only and you don't want them to be included in the result config file, you can declare them with `#var` keyword.

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

In the example below after the build process: datasource.url: jdbc:oracle:thin:@**100.30.162.80**:1521:ARMSDEV

 
**orders-db/application.dev.yaml** 
```yaml   
#include oracle-common
 
#var oracle.host: 100.30.162.80         
```
## Removing base properties
Using `#var` you can remove properties from the result config file. You can include some config and override any property with #var to exclude it from the result config file.

Let's remove 'payments.system.retries' property for 'dev' environment:

**db-client/application.yaml**
```yaml
datasource:
  minimum-pool-size: 2  
  maximum-pool-size: 5
```
**payments/application.yaml**
```yaml
#include db-client
#var datasource.minimum-pool-size:  // will not be included into result config       
```

## Placeholder's default value
You can specify a default value for a placeholder using the following syntax: ${component@property:**defaultValue**}

Let's set a default value for 'oracle host'

**oracle-common/application.yaml**
```yaml
datasource:
  maximum-pool-size: 10
  url: jdbc:oracle:thin:@${this@oracle.host:172.30.162.20}:1521:${this@oracle.sid}
#var oracle.sid: ARMSDEV
```
Note, a default value can be a placeholder:
 `${component@property:${component2@property7:Missing value}}`
 
In the example Microconfig will try to:
* resolve `${component@property}`
* if the above is missing - resolve `${component2@property7}`
* if the above is missing - return 'Missing value'

If a placeholder doesn't have a default value and that placeholder can't be resolved, Microconfig throws an exception with the detailed problem description.

## Specials placeholders
As we discussed the syntax for placeholders looks like `${component@property}`.
Microconfig has several special useful placeholders:

* `${this@env}` - returns the current environment name.
* `${...@name}` - returns the component's name.
* `${...@configDir}` - returns the full path of the component's config dir.
* `${...@resultDir}` - returns the full path of the component's destination dir (the resulting files will be put into this dir).
* `${this@configRoot}` - returns the full path of the config repository root dir (see `root` build param ).

There are some other environment descriptor related properties, we will discuss them later:
* `${...@ip}`
* `${...@portOffset}`
* `${...@group}`
* `${...@order}`

Note, if you use a special placeholder with `${this@...}` then the value will be context dependent. Let's apply `${this@name}` to see why it's useful.

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
To resolve environment variables use the following syntax: `${env@variableName}`

For example:
```
 ${env@Path}
 ${env@JAVA_HOME}
 ${env@NUMBER_OF_PROCESSORS}
```

To resolve Java system variables (System::getProperty) use the following syntax: `${system@variableName}`

Some useful standard system variables:

```
 ${system@user.home}
 ${system@user.name}
 ${system@os.name}
```

You can pass your own system properties during Microconfig start with `-D` prefix (See 'Running config build' section)

Example:
```
 -DtaskId=3456 -DsomeParam3=value
```
Then you can access it: `${system@taskId}` or `${system@someParam3}`

## Placeholders to another config type

As we discussed Microconfig supports different config types and detects the type by file extensions. Microconfig resolves placeholders based on properties of the **same config type** only.

Let’s see the example how it works:

‘Order-service’ has ‘service.port’ property in application.**yaml**, so you can declare a placeholder to this property from application config types only (*.yaml or *.properties). If you declare that placeholder in, for example, *.process files, Microconfig will not resolve it and throw an exception.

**someComponent/application.yaml**
```yaml
orderPort: ${order-service@server.port} # works
```
**someComponent/application.process**
```yaml
orderPort: ${order-service@server.port} # doesn’t work
```

If you need to declare a placeholder to a property from another config type you have to specify the config type using the following syntax: ${**configType**::component@property}.

For our example the correct syntax:

**someComponent/application.process**
```yaml
orderPort: ${app::order-service@server.port}
```

Microconfig default config types:
* `app` – for *.yaml or *.properties
* `process` – for *.proc or *.process
* `deploy` – for *.deploy
* `secret` – for *.secret
* `env` – for *.env

# Expression language
Microconfig supports math expressions, conditions and Java [String API](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html).

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
Microconfig allows specifying environment specific properties (add/remove/override). For instance, you want to increase the connection-pool-size for dbs and increase the amount of memory for prod env.
To add/remove/override properties for the environment, you can create application.**${ENVNAME}**.yaml file in the config folder. 

Let's override connection pool size for 'dev' and 'prod' and add one new param for 'dev'. 

```
order-db
└───application.yaml
└───application.dev.yaml
└───application.prod.yaml
```

**orders-db/application.dev.yaml**
```yaml   
datasource.maximum-pool-size: 15   
hibernate.show-sql: true
```

**orders-db/application.prod.yaml**
```yaml   
datasource.maximum-pool-size: 50
```

Also, you can declare common properties for several environments in a single file. You can use the following filename pattern: application.**${ENV1.ENV2.ENV3...}**.yaml

Let's create common properties for dev, dev2 and test environments.

```
order-db
└───application.yaml
└───application.dev.yaml
└───application.dev.dev2.test.yaml
└───application.prod.yaml
```

**orders-db/application.dev.dev2.test.yaml**
```yaml   
hibernate.show-sql: true
```

When you build config for a specific environment (for example 'dev') Microconfig will collect properties from:
* application.yaml 
* then add/override properties from application.dev.{anotherEnv}.yaml.
* then add/override properties from application.dev.yaml.
 
## Profiles and explicit environment names for includes and placeholders
As we discussed you can create environment specific properties using the filename pattern: application.${ENV}.yaml. You can use the same approach for creating profile specific properties.

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

But what if you want some services to be configured with a long timeout? Instead of the environment you can use the profile name in the filename:
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

And specify the profile name with include:

**payments/application.yaml**
```yaml
#include timeout-settings[long]
```

You can use the profile/environment name with placeholders as well:

```
${timeout-settings[long]@readTimeoutMs}
${kafka[test]@bootstrap-servers}
```

The difference between environment specific files and profiles is only logic. Microconfig handles it in the same way.

# Template files
Microconfig allows you to keep configuration files for any libraries in their specific format and resolve placeholders inside them.
For example, you want to keep logback.xml (or some other descriptor for your log library) and reuse this file with resolved placeholders for all your services. 

Let's create this file:
```
repo
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
So we want every service to have its own logback.xml with resolved `${application.name}`. 
Let's configure the order and payment services to use this template.

**orders/application.yaml**
```yaml
#include service-discovery-client

mc.template.logback.fromFile: ${logback@configDir}/logback.xml # full path to logback.xml, @configDir - special placeholder property
```

**payments/application.yaml**
```yaml
#include service-discovery-client

mc.template.logback.fromFile: ${logback@configDir}/logback.xml
```  
   
It's better to extract the common property `mc.template.logback.fromFile` to logback-template/application.yaml and then use #include.

```
repo
└───common
|   └───logback-template 
|   └───logback.xml
|   └───application.yaml
```    
**logback-template/application.yaml**
```yaml   
mc.template.logback.fromFile: ${logback@configDir}/logback.xml
```
**orders-template/application.yaml**
```yaml
#include service-discovery-client, logback-template
```
**payments-template/application.yaml**
```yaml
#include service-discovery-client, logback-template
```  

As we saw in the above text the order and payment services include the `application.name` property from service-discovery-client.
During the config build Microconfig will replace `${application.name}` inside logback.xml with the service's property value and copy the resulting file 'logback.xml' to the relevant folder for each service.

If you want to declare a property for a template only and don't want this property to be included into the result config file you can use `#var` keyword. 

If you want to override the template destination filename you can use `mc.template.${templateName}.toFile=${someFile}` property. For example:  
 
 **logback-template/application.yaml**
 ```yaml   
 mc.template.logback:
   fromFile: ${logback@configDir}/logback.xml
   toFile: logs/logback-descriptor.xml
 ``` 
 
You can use the absolute or the relative path for `toFile` property. The relative path starts from the resulting service config dir (see 'Running config build' section).

So the template dependency declaration syntax looks like:   
```
mc.template.${templateName}:
  fromFile: ${sourceTemplateFile}
  toFile: ${resolvedTemplateDestinationFile}
```
`${templateName}` - is used only for mapping `fromFile` and `toFile` properties.

Let's override the file that will be copied on the prod environment:
```
repo
└───common
|   └───logback-template 
|       └───logback.xml
|       └───logback-prod.xml
|       └───application.yaml
|       └───application.prod.yaml
```
**logback-template/application.prod.yaml**
```yaml   
mc.template.logback.fromFile: ${logback@configDir}/logback-prod.xml
``` 
## Mustache template engine support
If resolving placeholders inside templates is not enough for you, you can use [Mustache template engine](https://mustache.github.io/mustache.5.html). With Mustache you can use loops, conditions and includes.

Let's imagine we want to configure different Logback appenders on different environments. We can use condition 'appender.rolling' and override this value on different environments.

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
mc.mustache.logback.fromFile: ${logback@configDir}/logback.xml
#var appender.rolling: true
 ``` 

 **logback-template/application.test.yaml**
 ```yaml   
#var appender.rolling: false
 ``` 

Microconfig uses Mustache if the template declaration starts with `mc.mustache` prefix:
```
mc.mustache.logback.fromFile: ${logback@configDir}/logback.xml
```
or the template file has  `*.mustache` extension:  
``` 
mc.template.logback:
  fromFile: ${logback@configDir}/logback.mustache
  toFile: logger.xml
```

# Environment descriptor
As we discussed every service can have default and environment-specific configurations, also we can extract a common configuration to some components. 
During the build phase we want to build configs for a subset of our components, only for real services on a concrete environment.
Of course, you can pass the environment name and the list of service names as parameters to build the configuration. But this is not very convenient if you want to build configuration for a large number of services.

So Microconfig allows specifying a list of service names on a special environment descriptor and then use only the environment name to build configs for all services listed on that descriptor.

Environments descriptors must be in `${configRoot}/envs` folder.
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
    - order-service
    - order-ui

payments:
  components:
    - payment-db-patcher
    - payment-service
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
    - order-service # component name
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

You can use the optional param `ip` for the environment or component groups and then use it via `${componentName@ip}`.

For instance, `${order-service@ip}` will be resolved to 12.53.12.67, `${payment-ui@ip}` will be resolved to 170.53.12.80.   
```yaml
ip: 170.53.12.80 # default ip

orders:  
  ip: 12.53.12.67 # ip overridden for the group
  components:  
    - order-db-patcher
    - order-service
    - order-ui

payments:  
  components:
    - payment-db-patcher
    - payment-service
    - payment-ui    
```

Consider configuring your deployment tool to read the environment descriptor to know which services to deploy.

# Running the config build
As we discussed Microconfig has its own format for configuration sources. 
During the config build Microconfig inlines all includes, resolves placeholders, evaluates expression language, copies templates, and stores the result values into plain *.yaml or *.properties files to a dedicated folder for each service.

To run the build you can download Microconfig release from https://github.com/microconfig/microconfig/releases.

The required build params:
* `-r` - full or relative config root dir. 
* `-e` - environment name (environment is used as a config profile, also as a group of services to build configs).

Optional build params:
* `-d` - full or relative build destination dir. Default = ${currentFolder}/build
* `-stacktrace` - Show full stacktrace in case of exceptions. Values: true/false. Default: false  

To build configs not for the whole environment but only for specific services you can use the following optional params:
* `-g` - a comma-separated list of component groups to build configs. 
* `-s` - a comma-separated list of services to build configs. 

Command line params example (Java 8+ required):
```
java -jar microconfig.jar -r repo -e prod
```

To add system properties use `-D`
```
java -DtaskId=3456 -DsomeParam=value -jar microconfig.jar -r repo -d configs -e prod
```

To speed up the build up to 3 times you can add `-XX:TieredStopAtLevel=1` Java VM param. Although build time for even big projects with hundreds of services is about 1-2 seconds.
```
java -XX:TieredStopAtLevel=1 -jar microconfig.jar -r repo -e prod
```

Let's see examples of initial and destination folder layouts: 

Initial source layout:
```
repo
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
After build:
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

You can try to build configs from the dedicated example repo: https://github.com/microconfig/microconfig-quickstart 

## Viewing differences between config versions 
During the config build, Microconfig compares newly generated files to files generated during the previous build for each service for each config type.
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

The Diff format:

**diff-application.yaml**
```yaml     
+security.client.protocol: SSL # property has been added
-connection.timeoutMs: 1000 # property has been removed
 server.max-threads: 10 -> 35 # value has been changed from '10' to '35'
```

# YAML and Properties format support
Microconfig supports *.yaml and *.properties format for source and result configs.
You can keep a part of configuration in *.yaml files and another part in *.properties.

```
repo
└───core  
│   └───orders
│   │   └───application.yaml
│   │   └───process.proc
│   └───payments
│       └───application.properties
│       └───process.proc
```

Yaml configs can have nested properties:
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

Yaml format configs will be built into *.yaml, property configs will be built into *.properties. If *.properties configs include *.yaml configs, the resulting file will be *.yaml.
Microconfig can detect the format based on separators (if a config file has extension neither *.yaml nor *.properties). If you use `:` key-value separator, Microconfig will handle it like *.yaml (`=` for *.properties).

# Intellij IDEA plugin
To make configuration management a little bit easier you can use Microconfig Intellij IDEA plugin. The plugin can navigate to #include and placeholders' sources, show hints with resolved placeholders, and build configs from IDE.

See the documentation here: https://github.com/microconfig/microconfig-idea-plugin

# Contributing
If you want to contribute to the project and make it better, your help is very welcome. You can request a feature or submit a bug via issues. Or submit a pull request.

Contributing to the documentation is very welcome too.

Your Github 'Star' is appreciated!

https://github.com/microconfig/microconfig

# Contacts
[Join our Slack!](https://join.slack.com/t/microconfig/shared_invite/zt-dflf2m0n-wOPVfAmk5eiHPn_9Omff7Q)

support@microconfig.io
