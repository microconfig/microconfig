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
* Process configuration (configuration that is used to start your service, like memory limit, VM params, etc)
* Application configuration (configuration that you service reads after startup and use in runtime)
* OS ENV variables
* Lib specific templates (for instance, your logger specific descriptor (logback.xml), kafka.conf, cassandra.yaml, etc)
* Static files/scripts to run before/after you service start
* Secrets configuration (Note, you should not store in VCS any sensitive information, like passwords. In VCS you can store references(keys) to passwords, and keep password in special secured stores(like Vault) or at least in encrypted files on env machines)

# Service configuration 

Inside service folder you can create configuration in key=value format. 

Lets create basic application configuration files for each service. Microconfig treats *.properties like application properties. You can spit configuration among several files, but for simplity we will create sigle application.properties for eaech service.
(note result config file after build will be single dispite amount of base sourse files)

```
repo
└───core  
│    └───orders
│    │   └───application.properties
│    └───payments
│        └───application.properties
│	
└───infra
    └───service-discovery
    │   └───application.properties
    └───api-gateway
        └───application.properties
```
