# Micro-config engine

[![Build Status](https://travis-ci.com/Microconfig/microconfig.svg?branch=master)](https://travis-ci.com/Microconfig/microconfig)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This project is intended to make it easy and convenient to manage configuration for microservices(or just for big amount of services) and reuse common part.
If your project consists of tens or hundreds services you have to:
1) To be able to keep configuration for each service, ideally separately from code.
2) Configuration for different services can have common and specific parts. Also configuration for the same service on different environments can have common and specific parts as well.
3) Common part for different services (or for one service on different environments) should not be copy-pasted and must be easy to reuse.
4) Some configuration properties must be dynamic (calculated using expression language) using other properties.
