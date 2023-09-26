package com.geotab.sdk.textmessage;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;

import com.geotab.api.Api;
import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.param.EntityParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.Id;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.textmessage.CannedResponseContent;
import com.geotab.model.entity.textmessage.CannedResponseOption;
import com.geotab.model.entity.textmessage.LocationContent;
import com.geotab.model.entity.textmessage.TextContent;
import com.geotab.model.entity.textmessage.TextMessage;
import com.geotab.model.entity.user.User;
import com.geotab.model.login.LoginResult;
import com.geotab.model.search.TextMessageSearch;
import com.geotab.model.search.UserSearch;
import com.geotab.sdk.Util.Cmd;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTextMessageApp {

  private static final Logger log = LoggerFactory.getLogger(SendTextMessageApp.class);

  public static void main(String[] args) throws Exception {
    // Process command line arguments
    Cmd cmd = new Cmd(SendTextMessageApp.class);

    // Create the Geotab API object used to make calls to the server
    try (Api api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {

      // Authenticate user
      authenticate(api);

      // Get a device to send text messages to
      Device messageRecipient = getTargetDevice(api);
      log.info("Messages will be send to " + messageRecipient.getName());

      // Get the User who the messages will be sent from
      User sender = getUser(api, cmd.credentials.getUserName());
      log.info("Messages will be sent from {} ", sender.getName());

      // Sample TextMessage and reply
      sendBasicMessageAndMockDeviceReply(api, sender, messageRecipient);

      // Sample of sending a text message with a GPS location
      sendMessageWithGpsLocation(api, sender, messageRecipient);
    }
  }

  private static LoginResult authenticate(Api api) {
    log.debug("Authenticating…");

    LoginResult loginResult = null;

    // Authenticate user
    try {
      loginResult = api.authenticate();
      log.info("Successfully Authenticated");
    } catch (InvalidUserException exception) {
      log.error("Invalid user: ", exception);
      System.exit(1);
    } catch (DbUnavailableException exception) {
      log.error("Database unavailable: ", exception);
      System.exit(1);
    } catch (Exception exception) {
      log.error("Failed to authenticate user: ", exception);
      System.exit(1);
    }

    return loginResult;
  }

  private static Device getTargetDevice(Api api) {
    log.debug("Get 1 device…");
    try {
      Optional<List<Device>> devices = api.callGet(SearchParameters.searchParamsBuilder()
          .typeName("Device").resultsLimit(1).build(), Device.class);
      if (devices.isPresent() && !devices.get().isEmpty()) {
        return devices.get().get(0);
      }

      log.error("Datastore does not contain any devices !");
      System.exit(1);
    } catch (Exception exception) {
      log.error("Failed to get existing devices ", exception);
      System.exit(1);
    }

    return null;
  }

  private static User getUser(Api api, String userName) {
    log.debug("Get user {}…", userName);
    try {
      Optional<List<User>> users = api.callGet(SearchParameters.searchParamsBuilder()
          .typeName("User").search(UserSearch.builder().name(userName).build()).build(), User.class);

      if (users.isPresent() && !users.get().isEmpty()) {
        return users.get().get(0);
      }

      log.error("Could not find the user you are authenticated in as - {} !", userName);
      System.exit(1);
    } catch (Exception exception) {
      log.error("Failed to find user {} !", userName, exception);
      System.exit(1);
    }

    return null;
  }

  private static List<TextMessage> getTextMessages(Api api, LocalDateTime modifiedSince) {
    log.debug("Get TextMessages after {}…", modifiedSince);
    try {
      return api.callGet(SearchParameters.searchParamsBuilder().typeName("TextMessage")
              .search(TextMessageSearch.builder().modifiedSinceDate(modifiedSince).build()).build(), TextMessage.class)
          .orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get TextMessages after {} ", modifiedSince, exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static void sendBasicMessageAndMockDeviceReply(Api api, User sender,
      Device messageRecipient) {
    /*
     * Basic Message
     * A basic text message with a string message.
     */

    // Set up the message content
    TextContent messageContent = new TextContent("Testing: Geotab API example text message",
        false);

    // Construct the text message
    LocalDateTime nowUtc = nowUtcLocalDateTime();
    TextMessage basicTextMessage = TextMessage.builder()
        .sent(nowUtc)
        .delivered(nowUtc)
        .user(sender)
        .device(messageRecipient)
        .messageContent(messageContent)
        .isDirectionToVehicle(true)
        .build();

    // Add the text message. MyGeotab will take care of the actual sending.
    try {
      Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
          .typeName("TextMessage").entity(basicTextMessage).build());

      if (response.isPresent()) {
        log.info("Basic TextMessage added with id {} .", response.get().getId());
        basicTextMessage.setId(response.get());
      } else {
        log.warn("Basic TextMessage  not added; no id returned");
      }
    } catch (Exception exception) {
      // Catch and display any error that occur when adding the TextMessage
      log.error("Failed to import Basic TextMessage", exception);
      return;
    }

    /*
     * Canned Response Message
     * A canned response message is a text message with a list of predetermined responses
     * the receiver can select from.
     */

    // Example of sending a text message with canned a response.
    // Set up message and response options.
    CannedResponseContent cannedResponseContent = CannedResponseContent
        .cannedResponseContentBuilder()
        .message("Testing: Geotab API example text message with response options")
        .cannedResponseOptions(Lists.newArrayList(new CannedResponseOption("Ok")))
        .build();

    // Construct the text message.
    TextMessage textMessageWithResponses = TextMessage.builder()
        .device(messageRecipient)
        .messageContent(cannedResponseContent)
        .isDirectionToVehicle(true)
        .build();

    // Add the text message, Geotab will take care of the sending process.
    try {
      Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
          .typeName("TextMessage").entity(textMessageWithResponses).build());

      if (response.isPresent()) {
        log.info("Canned TextMessage added with id {} .", response.get().getId());
        textMessageWithResponses.setId(response.get());
      } else {
        log.warn("Canned TextMessage not added; no id returned");
      }
    } catch (Exception exception) {
      // Catch and display any error that occur when adding the TextMessage
      log.error("Failed to import Canned TextMessage", exception);
      return;
    }

    // Keep track of our last "known" sent date. We will send another message
    // but for the purpose of this example we are going to pretend it's from a Device.
    LocalDateTime lastKnownSentDate = nowUtc;

    //-------
    // START: MOCK A DEVICE REPLY.
    // **FOR EXAMPLE PURPOSES ONLY.**
    // THIS LOGIC IS HANDELED BY THE MYGEOTAB SYSTEM.
    // YOU WOULD NOT NORMALLY DO THIS IN A WORKING ENVIRONMENT.
    //-------

    // Here we are adding a new text message with "isDirectionToVehicle = false",
    // this means the message came from the device. Normally, these will be sent
    // by the Garmin device. This is just to show how to search for new responses.
    TextMessage textMessageFromDevice = TextMessage.builder()
        .sent(nowUtc)
        .delivered(nowUtc)
        .user(sender)
        .device(messageRecipient)
        .messageContent(TextContent.textContentBuilder()
            .message(cannedResponseContent.getCannedResponseOptions().get(0).getText())
            .urgent(false)
            .build())
        .parentMessage(textMessageWithResponses)
        .isDirectionToVehicle(true)
        .build();

    try {
      Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
          .typeName("TextMessage").entity(textMessageFromDevice).build());

      if (response.isPresent()) {
        log.info("Response TextMessage added with id {} .", response.get().getId());
        textMessageFromDevice.setId(response.get());
      } else {
        log.warn("Response TextMessage not added; no id returned");
      }
    } catch (Exception exception) {
      // Catch and display any error that occur when adding the TextMessage
      log.error("Failed to import Response TextMessage", exception);
    }

    //-------
    // END: MOCK A DEVICE REPLY
    //-------

    // Request any messages that have been delivered/sent/read since the date provided.
    List<TextMessage> textMessages = getTextMessages(api, lastKnownSentDate);
    log.info("{} TextMessages delivered/sent/read", textMessages.size());
  }

  private static void sendMessageWithGpsLocation(Api api, User sender,
      Device messageRecipient) {
    LocalDateTime lastKnownSentDate = nowUtcLocalDateTime();

    /*
     * Location Message
     * A location message is a message with a location.
     * A series of location messages can be sent in succession to comprise a route.
     * A clear message can be sent to clear any previous location messages.
     */

    // Example of sending a text message with a GPS location

    // Set up message and GPS location
    LocationContent clearStopsContent = LocationContent.locationContentBuilder()
        .message("Testing: Geotab API example clear all stops message")
        .address("Reset Stops")
        .latitude(0d)
        .longitude(0d)
        .build();

    // Construct a "Clear Previous Stops" message
    TextMessage clearMessage = TextMessage.builder()
        .device(messageRecipient)
        .user(sender)
        .messageContent(clearStopsContent)
        .isDirectionToVehicle(true)
        .build();

    // Add the clear stops text message, Geotab will take care of the sending process.
    try {
      Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
          .typeName("TextMessage").entity(clearMessage).build());

      if (response.isPresent()) {
        log.info("Clear Stop TextMessage added with id {} .", response.get().getId());
        clearMessage.setId(response.get());
      } else {
        log.warn("Clear Stop TextMessage not added; no id returned");
      }
    } catch (Exception exception) {
      // Catch and display any error that occur when adding the TextMessage
      log.error("Failed to import Clear Stop TextMessage", exception);
    }

    // Set up message and GPS location
    LocationContent withGpsLocation = LocationContent.locationContentBuilder()
        .message("Testing: Geotab API example location message")
        .address("Geotab")
        .latitude(43.452879d)
        .longitude(-79.701648d)
        .build();

    // Construct the location text message.
    TextMessage locationMessage = TextMessage.builder()
        .device(messageRecipient)
        .user(sender)
        .messageContent(withGpsLocation)
        .isDirectionToVehicle(true)
        .build();

    // Add the clear stops text message, Geotab will take care of the sending process.
    try {
      Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
          .typeName("TextMessage").entity(locationMessage).build());

      if (response.isPresent()) {
        log.info("Location TextMessage added with id {} .", response.get().getId());
        locationMessage.setId(response.get());
      } else {
        log.warn("Location TextMessage not added; no id returned");
      }
    } catch (Exception exception) {
      // Catch and display any error that occur when adding the TextMessage
      log.error("Failed to import Location TextMessage", exception);
    }

    // Request any messages that have been delivered/sent/read since the date provided.
    List<TextMessage> textMessages = getTextMessages(api, lastKnownSentDate);
    log.info("{} TextMessages delivered/sent/read", textMessages.size());
  }
}
