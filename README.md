# Java Examples

The following examples show common usages of the SDK using Java. We recommend that you study the examples to learn
everything necessary to build your own custom applications.

## How to run the examples?

In order to run these examples, you first need to install:

- JDK 11 or higher
- Maven 3.6.*

Then build the samples jar and execute the main class you need:

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git
> cd sdk-java-samples
> mvn clean verify
```

You could run it from your IDE or with the following command line:

```shell
> java -cp target/sdk-java-samples.jar;target/lib/* <mainClassWithPackage>
```
In Unix-based systems like Linux or macOS, you would use a colon (`:`) instead of a semicolon (`;`) as the path separator:

```shell
> java -cp "target/sdk-java-samples.jar:target/lib/*" <mainClassWithPackage>
```

## Examples list

### Get Logs

An example that obtains the logs for a given vehicle between a range of dates.

### Get Count

An example that obtains the count of entities. Enter the entity name (e.g. User, Zone, Device, etc) to get the count-of result.

### Text Message

An example that sends text messages to and from a GO device.

### Import Groups

A console example that is also a group import tool. It enables a one time import of groups to a database from a CSV
file.

### Import Devices

Another console example that imports devices from a CSV file.

### Import Users

Another console example that imports users from a CSV file.

### Data Feed

An example of retrieving GPS, Status and Fault data as a feed and exporting to a CSV file.
