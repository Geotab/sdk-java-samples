# Get Logs

This example demonstrates how to get GPS data (LogRecord) for a given vehicle (Device).

Steps:

1. Authenticate a user via login, password, database and server using the Geotab API object.
1. Search for a device by its serial number.
1. Get logs associated with the device for a given time period.

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.getlogs.GetLogsApp
```
