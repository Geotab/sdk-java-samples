# Import Devices

This is a console example of importing devices from a .csv file.

Steps:

1. Process command line arguments: Server, Database, Username, Password and Load .csv file.
1. Create Geotab API object and Authenticate.
1. Import devices into database.

> the .csv file included in this project is a sample, you may need to change entries (such as group names and serial numbers) for the example to work.

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

## CSV layout

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
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.importdevices.ImportDevicesApp
```
