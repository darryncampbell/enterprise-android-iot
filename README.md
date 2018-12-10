# Enterprise Android Iot

This repository contains the files and applications associated with connecting an Android device with cloud IOT platforms.

**Please see the associated tutorial** (*not yet available*) for full context

## Server-GCP

This folder contains the files required to deploy an IOT instance and associated processing on the Google Cloud Platform

## Server-AWS

This folder contains the files required to deploy an IOT instance and associated processing on the AWS Platform

## Server-Azure

This folder contains the files required to deploy an IOT instance and associated processing on the Azure Platform

## Client

This folder contains a simple test application which can be used to either:

- Test the connection to the selected cloud IOT instance (via MQTT)
- Publish a sample MQTT message over the test connection
- Publish real device data to the selected cloud IOT instance (via MQTT)

**Notes:**
The client has been tested on both consumer Android and Zebra enterprise devices, though not extensively.  Battery health, as reported by the client, is only available on Zebra devices as part of [Power Precision Plus](https://www.zebra.com/us/en/products/accessories/powerprecision-battery-solutions.html) 
