# Import Groups

This is a console example of importing groups from a .csv file.

Steps:

1. Process command line arguments: Server, Database, Username, Password, Input File and load .csv file.
1. Create Geotab API object and Authenticate.
1. Import groups into database.

> the .csv file included in this project is a sample, you may need to change entries (such as group names) for the example to work.

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

## CSV layout

Parent Group Name | New Group Name

```csv
# ImportGroups.csv
# Structure: <parent group name>,<new group name>
# Both <parent group name>,<new group name> must be unique 
# -------------------------------------------------------------------------
# lines beginning with '#' are comments and ignored
#
# create 2 groups under 'Organization'
# -------------------------------------------------------------------------
Organization,DriverGroup
Organization,VehicleGroups
```

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.importgroups.ImportGroupsApp
```
