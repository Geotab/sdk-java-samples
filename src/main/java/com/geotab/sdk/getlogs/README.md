# Get Logs

GPS logs (LogRecord) for a given vehicle (Device) over a date range.

Steps:
1. Authenticate via the Geotab API.
1. Search for a device by serial number.
1. Get logs for the device over the given time period.

## Run

```shell
mvn exec:java -Dapp=getLogs
```
