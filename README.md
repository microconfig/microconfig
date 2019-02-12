# Micro-config engine

[![Build Status](https://travis-ci.com/Microconfig/microconfig.svg?branch=master)](https://travis-ci.com/Microconfig/microconfig)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This project is intended to make it easy and convenient to manage configuration for microservices (or just for big amount of services) and reuse common part.

If your project consists of tens or hundreds services you have to:
* To be able to keep configuration for each service, ideally separately from code.
* Configuration for different services can have common and specific parts. Also configuration for the same service on different environments can have common and specific parts as well.
* Common part for different services (or for one service on different environments) should not be copy-pasted and must be easy to reuse.
* Some configuration properties must be dynamic (calculated using expression language) using other properties.

Microconfig is written in Java, but it designed to be used with systems written in any language. Microconfig just describes format of base configuration, syntax for placeholders, includes, excludes, overrides, expression language for dynamic properties and engine than can build it to plain *.properties or *.yaml. Also it can resolve placeholder in arbitrary template files.

Configuration can be built during deploy phase and result plain config files can be copied to filesystem, where your services can access it directly(for instance, Spring Boot can read configuration from *.properties), or you can distribute result configuration using any config servers (like [Spring cloud config server](https://spring.io/projects/spring-cloud-config))


