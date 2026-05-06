# Import Users

Import users from a CSV file.

Steps:
1. Authenticate via the Geotab API.
1. Load the CSV file.
1. Create users with organization and security nodes.
1. Import users into the database.

> The included CSV is a sample — update group names and password complexity as needed.

## CSV layout

```csv
# ImportUsers.csv
# Structure: User (Email), Password, Data Access, Security Clearance, First Name, Last Name
# Lines beginning with '#' are ignored.
BasicUser@company.com,5bJknaJPKJSKP62Z,Entire Organization,Administrator,Basic,User
```

## Run

```shell
mvn exec:java -Dapp=importUsers -DfilePath=src/main/java/com/geotab/sdk/importusers/ImportUsers.csv
```
