# Data Feed

Retrieves GPS, Status, Fault, Trip and Exception data as a continuous feed and exports to CSV or console.

The feed delegates processing to `DataFeedWorker`, which loads data and outputs results via `ConsoleExporter` (default) or `CsvExporter`. Swap or extend exporters to integrate with your own systems.

## Run

```shell
# credentials prompted interactively, runs one batch then exits
mvn exec:java -Dapp=dataFeed

# full example
mvn exec:java -Dapp=dataFeed \
  -Dserver=my.geotab.com -Ddatabase=database -Dusername=user@email.com \
  -DexportType=csv -DoutputFolder=/tmp/feed -DfeedContinuously=true
```

### Arguments

| Param | Required | Description |
|-------|----------|-------------|
| `server` | no | Server (e.g. `my.geotab.com`) |
| `database` | yes | Database |
| `username` | yes | Username |
| `password` | no | Password (prompted if omitted and no cached session) |
| `gpsToken` | no | Last known GPS token |
| `statusToken` | no | Last known status data token |
| `faultToken` | no | Last known fault data token |
| `tripToken` | no | Last known trip token |
| `exceptionToken` | no | Last known exception token |
| `exportType` | no | `console` (default) or `csv` |
| `outputFolder` | no | Output folder for CSV files (default: current directory) |
| `feedContinuously` | no | `true` to run indefinitely (default: `false`) |

## Feed output

### Console output

#### GPS data

| # | Field | Description | Example |
|---|-------|-------------|---------|
| 1 | Vehicle Serial Number | Unique serial number on the GO device | GT8010000001 |
| 2 | Date | UTC date/time of GPS position | 12/12/21 09:43:01 |
| 3 | Longitude | Decimal degrees | -80.6860275268555 |
| 4 | Latitude | Decimal degrees | 37.0907897949219 |
| 5 | Speed | km/h | 103 |

#### Status data

| # | Field | Description | Example |
|---|-------|-------------|---------|
| 1 | Vehicle Serial Number | | GT8010000001 |
| 2 | Date | UTC date/time | 12/12/21 09:43:01 |
| 3 | Diagnostic Name | Engine diagnostic description | Cranking Voltage |
| 4 | Source Name | Source of the reading | J1938 / Geotab Go |
| 5 | Controller Name | Controller for the source | Body Controller |
| 6 | Value | Reading value | 12.4 |
| 7 | Units | Unit of measure | Volts |

#### Fault data

| # | Field | Description | Example |
|---|-------|-------------|---------|
| 1 | Vehicle Serial Number | | GT8010000001 |
| 2 | Date | UTC date/time | 12/12/21 09:43:01 |
| 3 | Diagnostic Name | | Cranking Voltage |
| 4 | Failure Mode Name | Fault description | Voltage above normal |
| 5 | Failure Mode Source | Source of the fault | J1938 / Geotab Go |
| 6 | Controller Name | | Body Controller |

### CSV output

#### GPS data

| # | Field | Example |
|---|-------|---------|
| 1 | Vehicle Name | Truck 123 |
| 2 | Vehicle Serial Number | GT8010000001 |
| 3 | VIN | 1FUBCYCS111111111 |
| 4 | Date | 2012-07-13 20:36:36.000 |
| 5 | Longitude | -80.6860275268555 |
| 6 | Latitude | 37.0907897949219 |
| 7 | Speed | 103 |

#### Status data

| # | Field | Example |
|---|-------|---------|
| 1 | Vehicle Name | Truck 123 |
| 2 | Vehicle Serial Number | GT8010000001 |
| 3 | VIN | 1FUBCYCS111111111 |
| 4 | Date | 2012-07-13 20:36:36.000 |
| 5 | Diagnostic Name | Cranking Voltage |
| 6 | Diagnostic Code | 1234 |
| 7 | Source Name | J1938 |
| 8 | Controller Name | Body Controller |
| 9 | Value | 12.4 |
| 10 | Units | Volts |

#### Fault data

| # | Field | Example |
|---|-------|---------|
| 1 | Vehicle Name | Truck 123 |
| 2 | Vehicle Serial Number | GT8010000001 |
| 3 | VIN | 1FUBCYCS111111111 |
| 4 | Date | 2012-07-13 20:36:36.000 |
| 5 | Diagnostic Name | Cranking Voltage |
| 6 | Failure Mode Name | Voltage above normal |
| 7 | Failure Mode Code | 1234 |
| 8 | Failure Mode Source | J1938 |
| 9 | Controller Name | Body Controller |
| 10 | Count | 1 |
| 11 | Active | None / Pending / Active |
| 12 | Malfunction Lamp | 0 = off, 1 = on |
| 13 | Red Stop Lamp | 0 = off, 1 = on |
| 14 | Amber Warning Lamp | 0 = off, 1 = on |
| 15 | Protect Lamp | 0 = off, 1 = on |
| 16 | Dismiss Date | 2012-07-13 20:36:36.000 |
| 17 | Dismiss User | AUser@geotab.com |

#### Trip data

| # | Field | Example |
|---|-------|---------|
| 1 | Vehicle Name | Truck 123 |
| 2 | Vehicle Serial Number | GT8010000001 |
| 3 | VIN | 1FUBCYCS111111111 |
| 4 | Driver Name | Bob Goodman |
| 5 | Driver Keys | 100001, TA56EF2 |
| 6 | Trip Start Time | 2012-07-13 20:36:36.000 |
| 7 | Trip End Time | 2012-07-13 20:36:36.000 |
| 8 | Trip Distance (km) | 12 |

#### Exception data

| # | Field | Example |
|---|-------|---------|
| 1 | Id | 53CBB7C5-2DE4-4A84-8E0B-6E84C7D97FA9 |
| 2 | Vehicle Name | Truck 123 |
| 3 | Vehicle Serial Number | GT8010000001 |
| 4 | VIN | 1FUBCYCS111111111 |
| 5 | Diagnostic Name | Cranking Voltage |
| 6 | Diagnostic Code | 1234 |
| 7 | Source Name | J1938 |
| 8 | Driver Name | Bob Goodman |
| 9 | Driver Keys | 100001, TA56EF2 |
| 10 | Rule Name | Speeding |
| 11 | Active From | 2012-07-13 20:36:36.000 |
| 12 | Active To | 2012-07-13 20:36:36.000 |
