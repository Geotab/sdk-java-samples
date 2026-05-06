# Java SDK Samples

Common Geotab API usage examples in Java.

## Setup

```shell
git clone https://github.com/Geotab/sdk-java-samples.git
cd sdk-java-samples
mvn compile
```

## Run

```shell
mvn exec:java -Dapp=<name>
```

Credentials are prompted interactively on first run and cached in `session.local.properties` (gitignored). The last chosen app is remembered in `app.local.properties`.

## Examples

| App | Description |
|-----|-------------|
| [`getCount`](src/main/java/com/geotab/sdk/getcount/README.md) | Count of entities (User, Zone, Device…) |
| [`getLogs`](src/main/java/com/geotab/sdk/getlogs/README.md) | GPS logs for a vehicle over a date range |
| [`sendTextMessage`](src/main/java/com/geotab/sdk/textmessage/README.md) | Send text messages to/from a GO device |
| [`importGroups`](src/main/java/com/geotab/sdk/importgroups/README.md) | Import groups from a CSV file |
| [`importDevices`](src/main/java/com/geotab/sdk/importdevices/README.md) | Import devices from a CSV file |
| [`importUsers`](src/main/java/com/geotab/sdk/importusers/README.md) | Import users from a CSV file |
| [`dataFeed`](src/main/java/com/geotab/sdk/datafeed/README.md) | GPS, Status and Fault data feed to CSV |
| [`maintenance`](src/main/java/com/geotab/sdk/maintenance/) | Maintenance work orders and requests |
