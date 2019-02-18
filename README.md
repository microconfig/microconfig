# Microconfig overview and features

[![Build Status](https://travis-ci.com/microconfig/microconfig.svg?branch=master)](https://travis-ci.com/microconfig/microconfig)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Microconfig is intended to make it easy and convenient to manage configuration for microservices (or just for big amount of services) and reuse common part.

If your project consists of tens or hundreds services you have to:
* Keep configuration for each service, ideally separately from code.
* Configuration for different services can have common and specific parts. Also configuration for the same service on different environments can have common and specific parts as well.
* Common part for different services (or for one service on different environments) should not be copy-pasted and must be easy to reuse.
* It must be easy to understand how result file is generated and based on what placeholders are resolved. 
* Some configuration properties must be dynamic (calculated using expression language) using other properties.

Microconfig is written in Java, but it designed to be used with systems written in any language. Microconfig just describes format of base configuration, syntax for placeholders, includes, excludes, overrides, expression language for dynamic properties and engine than can build it to plain *.properties or *.yaml. Also it can resolve placeholders in arbitrary template files and show diff between config releases.

# Difference between Microconfig and other popular tools
Comparing to config servers (like Spring cloud config server or Zookeeper):

Config servers solve the problem of dynamic distribution of configuration in runtime (can use http api endpoints), but to distribute configuration you have to store it, ideally with change history and **without duplication of common part**. 

Comparing to Ansible:

Ansible is powerful but too general engine for deployment management and doesnt't provide common and clean way to store configuration for microservices. And a lot of teams have to invent their own solutions based on Ansible.

Microconfig does one thing and does it well. It provides approach, best practices and engine to keep configuration for big amount of services.

Your can use Microconfig together with config server and deployment frameworks. Configuration can be built during deploy phase and result plain config files can be copied to filesystem, where your services can access it directly(for instance, Spring Boot can read configuration from *.properties), or you can distribute result configuration using any config servers.
Also you can store not only application configuration but configuration how to run your services. And deployments frameworks can read configuration from Microconfig to start your services with right params and settings.    

# Where to store configuration
It’s a good practice to keep service configuration separated from code. It allows not to rebuild your services any time configuration is changed and use the same service artifacts (for instance, *.jar) for all environments, because it doesn’t contain any env specific configuration. Configuration can be updated even in runtime without service' source code changes.

So the best way to follow this principle is to have dedicated repository for configuration in your favorite version control system.  You can store configuration for all microservices in one repository to make it easy to reuse common part and be sure common part for services is consistent. 

# Basic folder layout
Let’s see basic folder layout that you can keep in a dedicated repository.

For every service you have to create folder with unique name(name of the service). In service directory we will keep common and env specific configuration.

So let’s image we have 4 microservices: order service, payment service,  service-discovery and api-gateway. To make it easy to manage we can group services by layers: 'infra' for infrastructure services and 'core' for our business domain services. The result layout will look like:

```
repo
└───core  
│    └───orders
│    └───payments
│	
└───infra
    └───service-discovery
    └───api-gateway
```

# Service configuration types

It convenient to have different kinds of configuration and keep it in different files:
* Process configuration (configuration that is used (by deployment tools) to start your service, like memory limit, VM params, etc. 
* Application configuration (configuration that your service reads after startup and use in runtime)
* OS ENV variables
* Lib specific templates (for instance, your logger specific descriptor (logback.xml), kafka.conf, cassandra.yaml, etc)
* Static files/scripts to run before/after your service start
* Secrets configuration (Note, you should not store in VCS any sensitive information, like passwords. In VCS you can store references(keys) to passwords, and keep password in special secured stores(like Vault) or at least in encrypted files on env machines)

# Service configuration files

Inside service folder you can create configuration in key=value format. 

Let’s create basic application and process configuration files for each service. 
Microconfig treats *.properties like application properties and *.proc like process properties.
You can split configuration among several files, but for simplity we will create single application.properties and process.proc for each service. Anyway after configuration build for each service for each config type a single result file will be generated despite amount of base source files.


```
repo
└───core  
│    └───orders
│    │   └───application.properties
│    │   └───process.proc
│    └───payments
│        └───application.properties
│        └───process.proc
│	
└───infra
    └───service-discovery
    │   └───application.properties
    │   └───process.proc
    └───api-gateway
        └───application.properties
        └───process.proc
```

Inside process.proc we will store configuration that describes what is your service and how to run it (Your config files can have other properties, so don't pay attention on concrete values).

**orders/process.proc**
```*.properties
    artifact=org.example:orders:19.4.2 # artifact in maven format groupId:artifactId:version
    java.main=org.example.orders.OrdersStarter # main class to run
    java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # vm params
```
**payments/process.proc**
```*.properties
    artifact=org.example:payments:19.4.2 # partial duplication
    java.main=org.example.payments.PaymentStarter
    java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # duplication
    instance.count=2
```
**service-discovery/process.proc**
```*.properties
    artifact=org.example.discovery:eureka:19.4.2 # partial duplication         
    java.main=org.example.discovery.EurekaStarter
    java.opts.mem=-Xms1024M -Xmx2048M # partial duplication         
```

As you can see we already have some small copy-paste (all services have 19.4.2 version, two of them have the same java.ops params).  Configuration duplication as bad as code one. We will see further how to do it better.

Let's see how application properties can look like. In comments we note what can be improved.

**orders/application.properties**
```*.properties
    server.port=9000
    application.name=orders # better to get name from folder
    orders.personalRecommendation=true
    statistics.enableExtendedStatistics=true
    service-discovery.url=http://10.12.172.11:6781 # are you sure url is consistent with eureka configuration?
    eureka.instance.prefer-ip-address=true  # duplication        
    datasource.minimum-pool-size=2  # duplication
    datasource.maximum-pool-size=10
    datasource.url=jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV  # partial duplication
    jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true  # duplication
```
**payments/application.properties**
```*.properties
    server.port=8080
    application.name=payments # better to get name from folder
    payments.booktimeoutInMs=900000 # how long in min ?
    payments.system.retries=3
    consistency.validateConsistencyIntervalInMs=420000 # difficult to read. how long in min ?
    service-discovery.url=http://10.12.172.11:6781 # are you sure url is consistent with eureka configuration?
    eureka.instance.prefer-ip-address=true  # duplication            
    datasource.minimum-pool-size=2  # duplication
    datasource.maximum-pool-size=5    
    datasource.url=jdbc:oracle:thin:@172.30.162.127:1521:ARMSDEV  # partial duplication
    jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true # duplication
```
**service-discovery/application.properties**
```*.properties
    server.port=6781
    application.name=eureka
    eureka.client.fetchRegistry=false
    eureka.server.eviction-interval-timer-in-ms=15000 # difficult to read
    eureka.server.enable-self-preservation=false    
```


The first bad thing - application files contain duplication. Also you have to spend some time to understand application’s dependencies or it structure. For instance, payments service contains settings for 1) service-discovery client,  2)for oracle db and 3)application specific. Of course you can separate group of settings by empty line. But we can do it more readable and understandable.


# Better config structure using #include
Our services have common configuration for service-discovery and database. To make it easy to understand service's dependencies, let’s create folders for service-discovery-client and oracle-client and specify links to these dependencies from core services.

```
repo
└───common
|    └───service-discovery-client 
|    | 	 └───application.properties
|    └───oracle-client
|        └───application.properties
|	
└───core  
│    └───orders
│    │   ***
│    └───payments
│        ***
│	
└───infra
    └───service-discovery
    │   ***
    └───api-gateway
        ***
```
**service-discovery-client/application.properties**
```*.properties
service-discovery.url=http://10.12.172.11:6781 # are you sure url is consistent with eureka configuration?
eureka.instance.prefer-ip-address=true 
```

**oracle-client/application.properties**
```*.properties
datasource.minimum-pool-size=2  
datasource.maximum-pool-size=5    
datasource.url=jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV  
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true
```

And replace explicit configs with includes

**orders/application.properties**
```*.properties
    #include service-discovry-client
    #include oracle-db-client
    
    server.port=9000
    application.name=orders # better to get name from folder
    orders.personalRecommendation=true
    statistics.enableExtendedStatistics=true    
```

**payments/application.properties**
```*.properties
    #include service-discovry-client
    #include oracle-db-client
    
    server.port=8080
    application.name=payments # better to get name from folder
    payments.booktimeoutInMs=900000 # how long in min ?
    payments.system.retries=3
    consistency.validateConsistencyIntervalInMs=420000 # difficult to read. how long in min ?    
```
Some problems still here, but we removed duplication and made it easy to understand service's dependencies.

You can override any properties from your dependencies.
Let's override order's connection pool size.

**orders/application.properties**
```*.properties        
    #include oracle-db-client
    datasource.maximum-pool-size=10
    ***    
```

Nice. But order-service has small part of its db configuration(pool-size), it not that bad, but we can make config semantically better.
Also as you could notice order and payment services have different ip for oracle.

order: datasource.url=jdbc:oracle:thin:@172.30.162.<b>31</b>:1521:ARMSDEV  
payment: datasource.url=jdbc:oracle:thin:@172.30.162.<b>127</b>:1521:ARMSDEV  
And oracle-client contains settings for .31.

Of course you can override datasource.url in payment/application.properties. But this overridden property will contain duplication of another part of jdbc url and you will get all standard copy-paste problems. We would like to override only part of property. 

Also it better to create dedicated configuration for order db and payment db. Both db configuration will include common-db config and override ip part of url.  After that we will migrate datasource.maximum-pool-size from orders service to order-db, so order service will contains only links to it dependecies and service specific configs.

Let’s refactor.
```
repo
└───common
|    └───oracle
|        └───oracle-common
|        |   └───application.properties
|        └───order-db
|        |   └───application.properties
|        └───payment-db
|            └───application.properties
```

**oracle-common/application.properties**
```*.properties
datasource.minimum-pool-size=2  
datasource.maximum-pool-size=5    
connection.timeoutInMs=300000
jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true
```
**orders-db/application.properties**
```*.properties
    #include oracle-common
    datasource.maximum-pool-size=10
    datasource.url=jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV #partial duplication
```
**payment-db/application.properties**
```*.properties
    #include oracle-common
    datasource.url=jdbc:oracle:thin:@172.30.162.127:1521:ARMSDEV #partial duplication
```

**orders/application.properties**
```*.properties
    #include order-db
    ***
```

**payments/application.properties**
```*.properties
    #include payment-db
```

# Env specific properties
Microconfg allows specifying env specific properties (add/remove/override). For instance you want to increase connection-pool-size for dbs and increase amount of memory for prod env.
To add/remove/override properties for env, you can create application.**${ENVNAME}**.properties file in config folder. 

Let's override connection pool connection size for dev and prod and add one new param for dev. 

```
order-db
└───application.properties
└───application.dev.properties
└───application.prod.properties
```

**orders-db/application.dev.properties**
```*.properties   
    datasource.maximum-pool-size=15    
```

**orders-db/application.prod.properties**
```*.properties   
    datasource.maximum-pool-size=50    
```

Also you can declare common properties for several environments on a single file.  You can use following file name pattern: application.**${ENV1.ENV2.ENV3...}**.properties
Let's create common props for dev, dev2 and test envs.

```
order-db
└───application.properties
└───application.dev.properties
└───application.dev.dev2.test.properties
└───application.prod.properties
```

**orders-db/application.dev.dev2.test.properties**
```*.properties   
    hibernate.show-sql=true    
```

When you build properties for specific env(for example 'dev') Microconfig will collect properties from:
* application.properties 
* then add/override properties from application.dev.{anotherEnv}.properties.
* then add/override properties from application.dev.properties.

# Placeholders

Instead of copy-paste value of some property Microconfig allows to placeholder to this value. 

Let's refactor service-discovery-client config.

Initial:

**service-discovery-client/application.properties**
```*.properties
service-discovery.url=http://10.12.172.11:6781 # are you sure host and port are consistent with SD configuration? 
```
**service-discovery/application.properties**
```*.properties
server.port=6761 
```

Refactored:

**service-discovery-client/application.properties**
```*.properties
service-discovery.url=http://${service-discovery@ip}:${service-discovery@server.port}
```
**service-discovery/application.properties**
```*.properties
server.port=6761
ip=10.12.172.11 
```

So if you change service-discovery port, all dependent services will get this update.

Microconfig has another approach to store service's ip. We will discuss it later. For now it better to set ip property inside service-discovery config file. 

Microconfig syntax for placeholders ${**componentName**@**propertyName**}. Microconfig forces to specify component name(folder). This syntax match better than just prop name 
(like ${serviceDiscoveryPortName}), because it makes it obvious based on what placeholder will be resolved and where to find initial placeholder value.

Let's refactor oracle db config using placeholders and env specific overrides.

Initial:

**oracle-common/application.properties**
```*.properties    
    datasource.maximum-pool-size=10
    datasource.url=jdbc:oracle:thin:@172.30.162.31:1521:ARMSDEV 
```   

Refactored:

**oracle-common/application.properties**
```*.properties    
    datasource.maximum-pool-size=10
    datasource.url=jdbc:oracle:thin:@${this@oracle.host}:1521:${this@oracle.sid}
    oracle.host=172.30.162.20    
    oracle.sid=ARMSDEV
```
**oracle-common/application.uat.properties**
```*.properties    
    oracle.host=172.30.162.80
```    
    
**oracle-common/application.prod.properties**
```*.properties    
    oracle.host=10.17.14.18    
    oracle.sid=ARMSPROD    
```        

As you can see using placeholders we can override not the whole property but only part of it. 

If you want to declare temp properties that will be used for placeholders and you don't want them to be included into result config file, you can declare them with #var keyword.

**oracle-common/application.properties**
```*.properties
    datasource.url=jdbc:oracle:thin:@${this@oracle.host}:1521:${this@oracle.sid}
    #var oracle.host=172.30.162.20    
    #var oracle.sid=ARMSDEV
```
**oracle-common/application.uat.properties**
```*.properties    
    #var oracle.host=172.30.162.80
``` 

This approach works with includes as well. You can #include oracle-common and then override oracle.host, and datasource.url will be resolved based of overridden value.

In the example below after build datasource.url=jdbc:oracle:thin:@**100.30.162.80**:1521:ARMSDEV
 
**orders-db/application.dev.properties** 
```*.properties   
     #include oracle-common    
     #var oracle.host=100.30.162.80                 
```  

# Placeholder's default value
You can specify default value for placeholder using syntax ${component@property:**defaultValue**}

Let's set default value for oracle host

**oracle-common/application.properties**
```*.properties    
    datasource.maximum-pool-size=10
    datasource.url=jdbc:oracle:thin:@${this@oracle.host:172.30.162.20}:1521:${this@oracle.sid}        
    #var oracle.sid=ARMSDEV
```
Note, default value can be a placeholder:
 ${component@property:${component2@property7:Missing value}}
 
Microconfig will try to:
* resolve `${component@property}`
* if it's missing - resolve `${component2@property7}`
* if it's missing - return 'Missing value'

If placeholder doesn't have a default value and that placeholder can't be resolved Microconfig throws exception with detailed exception description.    

# Removing  base properties
Using #var you can remove properties from result config file. You can include some config and override any property with #var to exclude it from result config file. 

Lets' remove 'payments.system.retries' property for dev env:

**payments/application.properties**
```*.properties
    payments.system.retries=3        
```
**payments/application.dev.properties**
```*.properties
    #var payments.system.retries=  // will not be included into result config        
```
# Specials placeholders
As we discussed syntax for placeholders looks like `${component@property}`.
Microconfig has several special useful placeholders:

* ${this@env} - returns current env name 
* ${...@name} - returns component's config folder name
* ${...@folder} - returns full path of component's config dir 
* ${this@configDir} - returns full path of root config dir   
* ${...@serviceDir} - returns full path of destination service dir (result files will be put into this dir)
* ${this@userHome} - returns full path of user home dir


There are some other env descriptor related properties, we will discuss them later:
* ${...@portOffset}
* ${...@ip}
* ${...@group}
* ${...@order}

Note, if you use special placeholders with ${this@...} than value will be context dependent. Lets's apply ${this@name} to see why it's useful.

Initial:

**orders/application.properties**
```*.properties
    #include service-discovery-client    
    application.name=orders    
```
**payments/application.properties**
```*.properties
    #include service-discovery-client    
    application.name=payments
```

Refactored:

**orders/application.properties**
```*.properties
    #include service-discovery-client    
```
**payments/application.properties**
```*.properties
    #include service-discovery-client
```
**service-discovery-client/application.properties**
```*.properties            
    application.name=${this@name}
```                 

# Env variables and system properties 
To resolve env variable use following syntax ${env@variableName}

For example:
```
 ${env@Path}
 ${env@JAVA_HOME}
 ${env@TEMP}
```

To resolve Java system variable (System::getProperty) use following syntax: ${system@variableName}

Some useful standard system variables:

```
 ${system@user.home}
 ${system@user.name}
 ${system@os.name}
```

You can pass your own system properties during Microconfig start with -D prefix (See 'Running config build' section)

Example:
```
 -DtaskId=3456 -DsomeParam=value
```
 
# Profiles and explicit env name for includes and placeholders
As we discussed you can create env specific properties using filename pattern: application.${ENV}.properties. You can use the same approach for creating profile specific properties.

For example you can create folder for http client timeout settings:

**timeout-settings/application.properties**
```*.properties    
    timeouts.connectTimeoutMs=1000    
    timeouts.readTimeoutMs=5000    
```
And some services can include this configuration:

**orders/application.properties**
```*.properties
    #include timeout-settings    
```
**payments/application.properties**
```*.properties
    #include timeout-settings
```

But what if you want some services to be configured with long timeout? Instead of env you can use profile name in filename:
```
timeout-settings
└───application.properties
└───application.long.properties
└───application.huge.properties
```
**timeout-settings/application.long.properties**
```*.properties
    timeouts.readTimeoutMs=30000    
```
**timeout-settings/application.huge.properties**
```*.properties
    timeouts.readTimeoutMs=600000    
```

And specify profile name with include:

**payments/application.properties**
```*.properties
    #include timeout-settings[long]
```

You can use profile/env name with placeholders too:

```
${timeout-settings[long]@readTimeoutMs}
${kafka[test]@bootstrap-servers}
```

The difference between env-specific files and profiles is only logical. Microconfig handles it the same way.  

# Expression language
Microconfig allows you to use powerful expression language. It's based on [Spring EL](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#expressions).    

Let's see some examples:

```*.properties
#Better than 300000
connection.timeoutInMs=#{5 * 60 * 1000}

#Microconfig placeholder and simple math
datasource.maximum-pool-size=#{${this@datasource.minimum-pool-size} + 10} 

#Using placeholder and Java String API
healthcheck.logSucessMarker=Started #{'${this@java.main}'.substring('${this@java.main}'.lastIndexOf('.') + 1)}

#Using Java import and Base64 API
sessionKey=#{T(java.util.Base64).getEncoder().encodeToString('Some value'.bytes)}  
```
Inside EL you can write any Java code in one line and Microconfig placeholders. Of course you shouldn't overuse it to keep configuration readable.

# Arbitrary template files
Microconfig allows to keep configuration files for any libraries with their specific format and resolve placeholders inside them.
For example you want to keep logback.xml (or some other descriptor for your log library) and reuse this file with resolved placeholders for all your services. 

Let's create this file:
```
repo
└───common
|    └───logback-template 
|     	 └───logback.xml
```
**logback-template/logback.xml**
```xml
<configuration>
    <appender class="ch.qos.logback.core.FileAppender">
        <file>logs/${application.name}.log</file>
            <encoder>
                <pattern>%d [%thread] %highlight(%-5level) %cyan(%logger{15}) %msg %n</pattern>
            </encoder>
    </appender>    
</configuration>
```
So we want every service to have its own logback.xml with resolved ${application.name}. 
Let's configure order and payment services to use this template.

**orders/application.properties**
```*.properties
    #include service-disconvery-client
    template.logback.fromFile=${logback@folder}/logback.xml # full path to logback.xml, @folder - special placeholder propertry
```

**payments/application.properties**
```*.properties
    #include service-disconvery-client
    template.logback.fromFile=${logback@folder}/logback.xml
```  
   
Better to extract common property to logback-template/application.properties and than use #include.

```
repo
└───common
|    └───logback-template 
|     	 └───logback.xml
|     	 └───application.properties
```    
**logback-template/application.properties**
```*.properties   
    template.logback.fromFile=${logback@folder}/logback.xml    
```
**orders-template/application.properties**
```*.properties
    #include service-disconvery-client
    #include logback-template
```
**payments-template/application.properties**
```*.properties
    #include service-disconvery-client
    #include logback-template
```  

As your could notice placeholder syntax inside template `${propName}`  differs from Micronfig one `${component@propName}`, it doesn't specify component name.
Micronconfig resolves template's placeholders based on properties from component which declared dependencies on template.

As we remember order and payment services include 'application.name' property from service-discovery-client.
During config build Microconfig will replace ${application.name} inside logback.xml with service's property value and copy result logback.xml to result folder for each service.

If you want to declare property for template only and don't want this property to be included into result config file you can use `#var propName=value`. 

If you want to specify template destination dir and file you can use `template.${templateName}.toFile=${someFile}` property. For example: 
 
 **logback-template/application.properties**
 ```*.properties   
     template.logback.fromFile=${logback@folder}/logback.xml    
     template.logback.toFile=logs/logback-descriptor.xml
 ``` 
 
You can use absolute or relative path for `toFile` property. Relative path starts from result service config dir (See 'Running config build' section).

So template dependency declaration syntax looks like:   
```
template.${templateName}.fromFile=${sourceTemplateFile}    
template.${templateName}.toFile=${resolvedTemplateDestinationFile}
```
`${templateName}` - is used only for mapping `fromFile` and `toFile` properties.

Let's override file that will be copied on prod env:
```
repo
└───common
|    └───logback-template 
|     	 └───logback.xml
|     	 └───logback-prod.xml
|     	 └───application.properties
|     	 └───application.prod.properties
```
**logback-template/application.prod.properties**
```*.properties   
    template.logback.fromFile=${logback@folder}/logback-prod.xml        
``` 

# Grouping different types of configuration
..todo write doc 
# Environment descriptor
..todo write doc
# Running config build
As we discussed Micronfig has its own format for configuration sources. 
During config build Micronfig inlines all includes, resolves placeholders, evaluates expression language, copies templates and stores result values into plain *.properties or *.yaml files to dedicated folder for each service.

To run build you can download Microconfig release from: https://github.com/microconfig/microconfig/releases.

Microconfig required build params:
* `root=` - full or relative config root dir. 
* `dest=` - full or relative build destination dir.
* `env=` - environment name (Environment is used as config profile, also as group of services to build configs for).

To build configs not for the whole environment but only for specific services you can use following optional params: 
* `group=` - comma separated list of component groups to build configs for. 
* `services=` - comma separated list of services to build configs for. 

Command line params example:
```
java -jar microconfig.jar root=repo dest=configs env=prod
```

To add system properties add -D:
```
java -jar microconfig.jar -DtaskId=3456 -DsomeParam=value root=repo dest=configs env=prod
```

Let's see example of initial and destination folder layouts: 

Initial source layout:
```
repo
└───common
|    └───logback-template 
|     	 └───logback.xml
└───core  
│    └───orders
│    │   └───application.properties
│    │   └───process.proc
│    └───payments
│        └───application.properties
│        └───process.proc
│	
└───infra
    └───service-discovery
    │   └───application.properties
    │   └───process.proc
    └───api-gateway
        └───application.properties
        └───process.proc
```
After build:
```
configs
└───orders
│   └───application.properties
│   └───process.properties
|   └───logback.xml
└───payments
│   └───application.properties
│   └───process.properties
|   └───logback.xml
└───service-discovery
│   └───application.properties
│   └───process.properties
|   └───logback.xml
└───api-gateway
    └───application.properties
    └───process.properties
    └───logback.xml
```

You can try to build configs from dedicated example repo: https://github.com/microconfig/configs-layout-example 

# Post config build callbacks
..todo write doc
# Viewing differences between config's versions
..todo write doc