# Import Users

This is a console example of importing users from a .csv file.

Steps:

1. Process command line arguments: Server, Database, Username, Password, Input File and load .csv file.
1. Create Geotab API object and Authenticate.
1. Create users.
1. Add organization and security nodes to the users.
1. Create Geotab API object and Authenticate.
1. Import users into database.

> the .csv file included in this project is a sample, you may need to change entries (such as group names or password complexity) for the example to work.

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

## CSV layout

email | password | data access | security clearance name | first name | last name

```csv
# ImportUsers.csv
# Structure: User (Email), Password, Data Access,Security Clearance,First Name,Last Name
# -------------------------------------------------------------------------
# lines beginning with '#' are comments and ignored

# Basic authentication users
BasicUser@company.com,5bJknaJPKJSKP62Z,Entire Organization,Administrator,Basic,User
```

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.importusers.ImportUsersApp
```
