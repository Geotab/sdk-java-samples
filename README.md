# Java Examples 

The following examples show common usages of the SDK using Java. We recommend that you study the examples to learn everything necessary to build your own custom applications.

## Geotab Java SDK jar installation

In case you are provided with a pre-release Geotab Java SDK jar, you will need to install it to your local maven repository. The jar received is built with maven, so it will contain inside it the pom.xml with dependencies.

Run this command from the same path where you have the Geotab Java SDK jar (this example assumes version 1.0-SNAPSHOT )
```shell
> mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=java-sdk-1.0-SNAPSHOT.jar
```

The output of the above command should look like this, informing about jar and pom installation
```shell
>  [INFO] --- maven-install-plugin:3.0.0-M1:install-file (default-cli) @ standalone-pom ---
>  [INFO] Installing ...\java-sdk-1.0-SNAPSHOT.jar to ...\.m2\repository\com\geotab\java-sdk\1.0-SNAPSHOT\java-sdk-1.0-SNAPSHOT.jar
>  [INFO] Installing ...\AppData\Local\Temp\java-sdk-1.0-SNAPSHOT687252861701726785.pom to ...\.m2\repository\com\geotab\java-sdk\1.0-SNAPSHOT\java-sdk-1.0-SNAPSHOT.pom
>  [INFO] ------------------------------------------------------------------------
>  [INFO] BUILD SUCCESS
>  [INFO] ------------------------------------------------------------------------
```

Update the samples project pom.xml with the Geotab Java SDK jar version just installed
```xml
  <properties>
    ...
    <geotab.sdk.version>1.0-SNAPSHOT</geotab.sdk.version>
    ...
  </properties>
```


## How to run the examples?

In order to run these examples, you first need to install:
- JDK 1.8 or higher
- Maven 3.6.*

Then build the samples jar and execute the main class you need:
```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean install
> cd target/
java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*' <mainClassWithPackage> 'my.geotab.com' 'database' 'user@email.com' 'password'
```


## Examples list

### Get Logs

An example that obtains the logs for a given vehicle between a range of dates.

### Text Message

An example that sends text messages to and from a GO device.

### Import Groups

A console example that is also a group import tool. It enables a one time import of groups to a database from a CSV file.

### Import Devices

Another console example that imports devices from a CSV file.

### Import Users

Another console example that imports users from a CSV file.

### Data Feed

An example of retrieving GPS, Status and Fault data as a feed and exporting to a CSV file.