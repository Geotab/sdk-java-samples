# Send Text Messages

This is a Geotab API console example of sending text messages. It illustrates how to send a basic message, canned
response message and location message.

Steps:

1. Process command line arguments: Server, Database, Username and Password.
1. Create Geotab API object and Authenticate.
1. Send a basic text message.
1. Send a canned response Text Message.
1. Send an GPS location Text Message.

## Prerequisites

The sample application requires:

- JDK 11 or higher
- Maven 3.6.*

## Getting started

```shell
> git clone https://github.com/Geotab/sdk-java-samples.git sdk-java-samples
> cd sdk-java-samples
> mvn clean verify
> java -cp target/sdk-java-samples.jar com.geotab.sdk.textmessage.SendTextMessageApp
```
