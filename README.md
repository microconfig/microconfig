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

Microconfig is written in Java, but it designed to be used with systems written in any language. Microconfig just describes format of base configuration, syntax for placeholders, includes, excludes, overrides, expression language for dynamic properties and engine than can build it to plain *.properties or *.yaml. Also it can resolve placeholders in arbitrary template files.

Configuration can be built during deploy phase and result plain config files can be copied to filesystem, where your services can access it directly(for instance, Spring Boot can read configuration from *.properties), or you can distribute result configuration using any config servers (like [Spring cloud config server](https://spring.io/projects/spring-cloud-config))

# How to keep configuration
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
* Application configuration (configuration that you service reads after startup and use in runtime)
* OS ENV variables
* Lib specific templates (for instance, your logger specific descriptor (logback.xml), kafka.conf, cassandra.yaml, etc)
* Static files/scripts to run before/after you service start
* Secrets configuration (Note, you should not store in VCS any sensitive information, like passwords. In VCS you can store references(keys) to passwords, and keep password in special secured stores(like Vault) or at least in encrypted files on env machines)

# Service configuration 

Inside service folder you can create configuration in key=value format. 

Let’s create basic application and process configuration files for each service. 
Microconfig treats *.properties like application properties and *.proc like process properties.
You can spit configuration among several files, but for simplity we will create single application.properties and process.proc for each service. Anyway after configuration build for each service for each config type a single result file will be generated despite amount of base source files.


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

Inside process.proc we will store configuration that describe what is your service and how to run it (You config files can have other properties, so don't pay attention on concrete values).

**orders process.proc:**
```*.properties
    artifact=org.example:orders:19.4.2 # artifact in maven format groupId:artifactId:version
    java.main=org.example.orders.OrdersStarter # main class to run
    java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # vm params
```
**payments process.proc:**
```*.properties
    artifact=org.example:payments:19.4.2 # partial duplication
    java.main=org.example.payments.PaymentStarter
    java.opts.mem=-Xms1024M -Xmx2048M -XX:+UseG1GC -XX:+PrintGCDetails -Xloggc:logs/gc.log # duplication
    instance.count=2
```
**service-discovery process.proc:**
```*.properties
    artifact=org.example.discovery:eureka:19.4.2 # partial duplication         
    java.main=org.example.discovery.EurekaSterter
    java.opts.mem=-Xms1024M -Xmx2048M # partial duplication         
```

As you can see we already have some small copy-paste (all services have 19.4.2 version, two of them have the same java.ops params).  Configuration duplication as bad as code one. We will see father how to do it better.

Application properties can look like:

**orders application.properties:**
```*.properties
    server.port=9000
    application.name=orders # better to get name from folder
    orders.personalRecommendation=true
    statistics.enableExtendedStatistics=true
    service-discovery.url=http://10.12.172.11:6781 # duplication
    eureka.instance.prefer-ip-address=true  # duplication        
    datasource.minimum-pool-size=2  # duplication
    datasource.maximum-pool-size=10    
    datasource.url=oracle.jdbc.url=jdbc:oracle:thin:@172.30.162.4:$1521:ARMSDEV  # partial duplication
    jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true  # duplication
```
**payments application.properties:**
```*.properties
    server.port=8080
    application.name=payments # better to get name from folder
    payments.booktimeoutInSec=900 # how long in min ?
    payments.system.retries=3
    consistency.validateConsistencyIntervalInMs=420000 # how long in min ?
    service-discovery.url=http://10.12.172.11:6781 # duplication
    eureka.instance.prefer-ip-address=true  # duplication            
    datasource.minimum-pool-size=2  # duplication
    datasource.maximum-pool-size=5    
    datasource.url=oracle.jdbc.url=jdbc:oracle:thin:@172.30.162.3:1521:ARMSDEV  # partial duplication
    jpa.properties.hibernate.id.optimizer.pooled.prefer_lo=true # duplication
```
**service-discovery application.properties:**
```*.properties
    server.port=6781
    application.name=eureka # better to get name from folder
    eureka.client.fetchRegistry=false
    eureka.server.eviction-interval-timer-in-ms=15000 # how long in sec ?
    eureka.server.enable-self-preservation=false    
```
