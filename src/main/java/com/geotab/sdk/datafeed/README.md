# Data Feed

The Data Feed is an example application that allows a third party to easily receive all the telematics data from your
devices. The application can be run interactively or in the background. The application will produce easy to consume CSV
files containing the key telematics data sets with updates every few seconds. Furthermore, the application can easily be
updated and customized when further integration is required for example, pushing the data into a Web service, writing to
a database, etc..

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

The Geotab Data Feed application connects to the MyGeotab cloud hosting services, please ensure that devices have been
registered and added to the database. The following information is required:

- Server (my.geotab.com)
- Username
- Password
- Database (customer)

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.datafeed.DataFeedApp --s 'server' --d 'database' --u 'user' --p 'password' --gt 'nnn' --st 'nnn' --ft 'nnn' --tt 'nnn' --et 'nnn' --exp 'csv' --f 'file path' --c
```

The application will bring up the following console:

```shell
> java -cp sdk-java-samples.jar com.geotab.sdk.datafeed.DataFeedApp --s 'server' --d 'database' --u 'user' --p 'password' --gt 'nnn' --st 'nnn' --ft 'nnn' --tt 'nnn' --et 'nnn' --exp 'csv' --f 'file path' --c
--s  The Server
--d  The Database
--u  The User
--p  The Password
--gt [optional] The last known gps data token
--st [optional] The last known status data token
--ft [optional] The last known fault data token
--tt [optional] The last known trip token
--et [optional] The last known exception token
--exp  [optional] The export type: console, csv. Defaults to console. [Not Implemented Yet]
--f  [optional] The folder to save any output files to, if applicable. Defaults to the current directory.
--c  [optional] Run the feed continuously. Defaults to false.
```

Example usage:

```shell
> java -cp sdk-java-samples.jar com.geotab.sdk.datafeed.DataFeedApp --s 'my.geotab.com' --d 'database' --u 'user@email.com' --p 'password' --c
```

The options above are the inputs that the feed example can take. A server, database, user and password must be supplied
in order for the feed to run. Optionally a gps data token, status data token, fault data token, trip token and/or
exception token can be provided to start the feed at a particular token version ("nnn" should be replaced with the known
token). Finally the feed can be instructed to run continuously or only one time.

By default, the feed will output its results to a CSV file in the location specified by the -f flag above. If no
location is provided the CSV file will be placed in the same directory where the app is located.

The feed example contains numerous other examples of what can be done with the feed output, for example writing the data
to the console. Developers are encouraged to take a look at the examples in order to understand how the options
available to them and how to best to integrate the feed data into their existing systems.

## Feed output

### Console output

#### GPS data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Serial Number | The unique serial number printed on the GO device. | GT8010000001 |
| 2 | Date | The date and time in UTC for the GPS position. | 12/12/21 09:43:01 |
| 3 | Longitude | The coordinate longitude in decimal degrees. | -80.6860275268555 |
| 4 | Latitude | The coordinate latitude in decimal degrees. | 37.0907897949219 |
| 5 | Speed | The speed in km/h. | 103 |

#### Status data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 2 | Date | The date and time in UTC for the engine diagnostic reading. | 12/12/21 09:43:01 |
| 3 | Diagnostic Name | The engine diagnostic description in English | Cranking Voltage |
| 4 | Source Name | An indication what the source of this status data reading is. | J1938 or J1708 or Geotab Go etc. |
| 5 | Controller Name | The controller name for the given source. | Body Controller |
| 6 | Value | The value associated with the status data reading. | 12.4 |
| 7 | Units | The unit of measure associated with this reading. | Volts |

#### Fault data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 2 | Date | The date and time in UTC for the engine diagnostic reading | 12/12/21 09:43:01 |
| 3 | Diagnostic Name | The engine diagnostic description in English | Cranking Voltage |
| 4 | Failure Mode Name | The fault description in English | Voltage above normal or shorted high, Out of Calibration |
| 5 | Failure Mode Source | An indication what the source of this fault reading is | J1938 or J1708 or Geotab Go etc |
| 6 | Controller Name | The controller name for the given source | Body Controller |

### CSV output

#### GPS data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Name | The vehicle name/description as displayed to users in Checkmate | Truck 123 |
| 2 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 3 | VIN | The Vehicle Identification Number of the vehicle | 1FUBCYCS111111111 |
| 4 | Date | The date and time in UTC for the GPS position | 2012-07-13 20:36:36.000 |
| 5 | Longitude | The coordinate longitude in decimal degrees | -80.6860275268555 |
| 6 | Latitude | The coordinate latitude in decimal degrees | 37.0907897949219 |
| 7 | Speed | The speed in km/h | 103 |

#### Status data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Name | The vehicle name/description as displayed to users in Checkmate | Truck 123 |
| 2 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 3 | VIN | The Vehicle Identification Number of the vehicle | 1FUBCYCS111111111 |
| 4 | Date | The date and time in UTC for the engine diagnostic reading | 2012-07-13 20:36:36.000 |
| 5 | Diagnostic Name | The engine diagnostic description in English | Cranking Voltage |
| 6 | Diagnostic Code | The numeric value associated with a diagnostic | 1234 |
| 7 | Source Name | An indication what the source of this status data reading is | J1938 or J1708 or Geotab Go etc. |
| 8 | Controller Name | The controller name for the given source | Body Controller |
| 9 | Value | The value associated with the status data reading | 12.4 |
| 10 | Units | The unit of measure associated with this reading | Volts |

#### Fault data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Name | The vehicle name/description as displayed to users in Checkmate | Truck 123 |
| 2 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 3 | VIN | The Vehicle Identification Number of the vehicle | 1FUBCYCS111111111 |
| 4 | Date | The date and time in UTC for the engine diagnostic reading | 2012-07-13 20:36:36.000 |
| 5 | Diagnostic Name | The engine diagnostic description in English | Cranking Voltage |
| 6 | Failure Mode Name | The fault description in English | Voltage above normal or shorted high, Out of Calibration |
| 7 | Failure Mode Code | The numeric value associated with a fault | 1234 |
| 8 | Failure Mode Source | An indication what the source of this fault reading is | J1938 or J1708 or Geotab Go etc |
| 9 | Controller Name | The controller name for the given source | Body Controller |
| 10 | Count | The number of times the fault occurred | 1 |
| 11 | Active | Represents a fault code state code from the engine system of the specific device | None, Pending, Active |
| 12 | Malfunction Lamp | Indicates if the malfunction lamp is on or off | 0 = off 1 = on |
| 13 | Red Stop Lamp | Indicates if the red stop lamp is on or off | 0 = off 1 = on |
| 14 | Amber Warning Lamp | Indicates if the amber warning lamp is on or off | 0 = off 1 = on |
| 15 | Protect Lamp | Indicates if the protect lamp is on or off | 0 = off 1 = on |
| 16 | Dismiss Date | The date and time a user dismissed the fault | 2012-07-13 20:36:36.000 |
| 17 | Dismiss User | The user who dismissed the fault | AUser@geotab.com |

#### Trip data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Vehicle Name | The vehicle name/description as displayed to users in Checkmate | Truck 123 |
| 2 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 3 | VIN | The Vehicle Identification Number of the vehicle | 1FUBCYCS111111111 |
| 4 | Driver Name | The name of the drive (if there was one) for the trip | Bob Goodman |
| 5 | Driver Keys | The key id of the driver (if there was one) for the trip | 100001, TA56EF2 |
| 6 | Trip Start Time | The date and time the trip started | 2012-07-13 20:36:36.000 |
| 7 | Trip End Time | The date and time the trip ended | 2012-07-13 20:36:36.000 |
| 8 | Trip Distance | The total distance of the trip in kilometers | 12 |

#### Exception data

| **#** | **Field Name** | **Description** | **Example** |
| --- | --- | --- | --- |
| 1 | Id | The unique identifier of the exception | 53CBB7C5-2DE4-4A84-8E0B-6E84C7D97FA9 |
| 2 | Vehicle Name | The vehicle name/description as displayed to users in Checkmate | Truck 123 |
| 3 | Vehicle Serial Number | The unique serial number printed on the GO device | GT8010000001 |
| 4 | VIN | The Vehicle Identification Number of the vehicle | 1FUBCYCS111111111 |
| 5 | Diagnostic Name | The engine diagnostic description in English | Cranking Voltage |
| 6 | Diagnostic Code | The numeric value associated with a diagnostic | 1234 |
| 7 | Source Name | An indication what the source of this status data reading is | J1938 or J1708 or Geotab Go etc |
| 8 | Driver Name | The name of the drive (if there was one) for the trip | Bob Goodman |
| 9 | Driver Keys | The key id of the driver (if there was one) for the trip | 100001, TA56EF2 |
| 10 | Rule Name | The name of the rule that was broken to generate this exception event | Speeding, Idling |
| 11 | Active From | The date and time the exception started | 2012-07-13 20:36:36.000 |
| 12 | Active To | The date and time the exception ended | 2012-07-13 20:36:36.000 |

## Customization

The feed has been designed in such a way that the data returned from the feed can be processed in a completely
customized manner. Within the DataFeedApp.java file is the feed executable as described above. It delegates the
processing to the DataFeedWorker.java, which loads the data of a feed and outputs the results. By default the
ConsoleExporter.java class is used to write the feed results to the console, however the developer can change this
method to customize the format of the output results to CSV. In this manner the developer can easily integrate the feed
with existing systems.
