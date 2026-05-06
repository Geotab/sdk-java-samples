# Import Devices

Import devices from a CSV file.

Steps:
1. Authenticate via the Geotab API.
1. Load the CSV file.
1. Import devices into the database.

> The included CSV is a sample — update serial numbers and group names as needed.

## CSV layout

```csv
# ImportDevices.csv
# Structure: <description>, <serialNumber>, <group1|group2>
# lines beginning with '#' are comments and ignored
Vehicle 1,GT-810-000-0001,Company Group
Vehicle 2,GT-820-000-0002,Company Group
```

## Run

```shell
mvn exec:java -Dapp=importDevices -DfilePath=src/main/java/com/geotab/sdk/importdevices/ImportDevices.csv
```
