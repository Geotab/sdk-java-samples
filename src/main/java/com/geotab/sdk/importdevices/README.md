# Import Devices

This is a console example of importing devices from a .csv file.

Steps:

1. Process command line arguments: Server, Database, Username, Password and Load .csv file.
1. Create Geotab API object and Authenticate.
1. Import devices into database.

> the .csv file included in this project is a sample, you may need to change entries (such as group names and serial numbers) for the example to work.

## Prerequisites

The sample application requires:

- JDK 1.8 or higher
- Maven 3.6.*


## CSV layout

description | serial number | pipe delimited group names

```csv
# ImportDevices.csv
# Structure: <description>, <serialNumber>, <group1|group2>
# -------------------------------------------------------------------------
# lines beginning with '#' are comments and ignored
Vehicle 1,GT-810-000-0001,Company Group
Vehicle 2,GT-820-000-0002,Company Group
```

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean install
> cd target/
> WINDOWS:  java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*' com.geotab.sdk.importdevices.ImportDevicesApp 'my.geotab.com' 'database' 'user@email.com' 'password' 'inputFileLocation'
> LINUX:    java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar:./lib/*' com.geotab.sdk.importdevices.ImportDevicesApp 'my.geotab.com' 'database' 'user@email.com' 'password' 'inputFileLocation'
```

### Parameters

`java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*' com.geotab.sdk.importdevices.ImportDevicesApp 'my.geotab.com' 'database' 'user@email.com' 'password' 'inputFileLocation'`

| **Name** | **Description** | **Required** | 
| --- | --- | --- |
| server | The server name (Example: my.geotab.com) | true |
| database | The database name (Example: G560) | true | 
| username | The MyGeotab user name | true |
| password | The MyGeotab password | true |
| inputFileLocation | Location of the CSV file to import. | true |
