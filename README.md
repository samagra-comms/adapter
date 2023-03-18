![Maven Build](https://github.com/samagra-comms/adapter/actions/workflows/build.yml/badge.svg)
![Github Package](https://github.com/samagra-comms/adapter/actions/workflows/build-deploy.yml/badge.svg)

# Overview
Adapters convert information provided by channels (SMS, Whatsapp) for each specific provider to xMessages and vice versa. Adapters are gateway to the external services and are responsible for receiving user response and sending the response to users.

# Getting Started

## Prerequisites

* java 11 or above
* docker
* kafka
* postgresql
* redis
* fusion auth
* lombok plugin for IDE
* maven

## Build
* build with tests run using command **mvn clean install -U**
* or build without tests run using command **mvn clean install -DskipTests**

# Detailed Documentation
[Click here](https://uci.sunbird.org/use/developer/uci-basics)