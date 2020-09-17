# Get Logs

 This example demonstrates how to get GPS data (LogRecord) for a given vehicle (Device).

Steps:

1. Authenticate a user via login, password, database and server using the Geotab API object.
1. Search for a device by its serial number.
1. Get logs associated with the device for a given time period.

## Prerequisites

The sample application requires:

- JDK 1.8 or higher
- Maven 3.6.*

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean install
> cd target/
> WINDOWS:  java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*' com.geotab.sdk.getlogs.GetLogsApp 'my.geotab.com' 'database' 'user@email.com' 'password' 'G9SERIALNO'
> LINUX:    java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar:./lib/*' com.geotab.sdk.getlogs.GetLogsApp 'my.geotab.com' 'database' 'user@email.com' 'password' 'G9SERIALNO'
```
