# Import Groups

Import groups from a CSV file.

Steps:
1. Authenticate via the Geotab API.
1. Load the CSV file.
1. Import groups into the database.

> The included CSV is a sample — update group names as needed.

## CSV layout

```csv
# ImportGroups.csv
# Structure: <parent group name>,<new group name>
# Both names must be unique. Lines beginning with '#' are ignored.
Organization,DriverGroup
Organization,VehicleGroups
```

## Run

```shell
mvn exec:java -Dapp=importGroups -DfilePath=src/main/java/com/geotab/sdk/importgroups/ImportGroups.csv
```
