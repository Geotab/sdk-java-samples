package com.geotab.sdk.textmessage;

import static com.geotab.plain.Entities.DeviceEntity;
import static com.geotab.plain.Entities.TextMessageEntity;
import static com.geotab.plain.Entities.UserEntity;
import static com.geotab.util.Util.apply;
import static java.lang.System.out;

import com.geotab.api.Api;
import com.geotab.plain.objectmodel.CannedResponseContent;
import com.geotab.plain.objectmodel.CannedResponseOption;
import com.geotab.plain.objectmodel.Device;
import com.geotab.plain.objectmodel.DeviceSearch;
import com.geotab.plain.objectmodel.LocationContent;
import com.geotab.plain.objectmodel.TextContent;
import com.geotab.plain.objectmodel.TextMessage;
import com.geotab.plain.objectmodel.TextMessageSearch;
import com.geotab.plain.objectmodel.User;
import com.geotab.plain.objectmodel.UserSearch;
import com.geotab.sdk.Util.Cmd;
import java.time.Instant;
import java.util.List;

public class SendTextMessageApp {

  public static void main(String[] args) throws Exception {
    var cmd = new Cmd(SendTextMessageApp.class);

    try (Api api = cmd.newApi()) {
      var messageRecipient =
        api.callGet(DeviceEntity, new DeviceSearch(), 1).orElseThrow().getFirst();
      out.format("Messages will be sent to %s%n", messageRecipient.getName());

      var sender = api
        .callGet(UserEntity, apply(new UserSearch(), s -> s.name = cmd.credentials.getUserName()), 1)
        .orElseThrow().getFirst();
      out.format("Messages will be sent from %s%n", sender.getName());

      sendBasicMessageAndMockDeviceReply(api, sender, messageRecipient);
      sendMessageWithGpsLocation(api, sender, messageRecipient);
    }
  }

  private static void sendBasicMessageAndMockDeviceReply(Api api, User sender, Device messageRecipient) {
    // Basic Message: A basic text message with a string message.
    var nowUtc = Instant.now();
    var basicTextMessage = apply(new TextMessage(), m -> {
      m.sent = nowUtc;
      m.delivered = nowUtc;
      m.user = sender;
      m.device = messageRecipient;
      m.messageContent =
        apply(
          new TextContent(),
          c -> {
            c.message = "Testing: Geotab API example text message";
            c.urgent = false;
          });
      m.isDirectionToVehicle = true;
    });

    var basicId = api.callAdd(TextMessageEntity, basicTextMessage).orElseThrow();
    out.format("Basic TextMessage added with id %s%n", basicId.getId());
    basicTextMessage.setId(basicId);

    /*
     * Canned Response Message
     * A canned response message with a list of predetermined responses the receiver can select from.
     */
    var cannedResponseContent = apply(new CannedResponseContent(), c -> {
      c.message = "Testing: Geotab API example text message with response options";
      c.cannedResponseOptions =
        List.of(apply(new CannedResponseOption(), o -> o.text = "Ok"));
    });

    var textMessageWithResponses = apply(new TextMessage(), m -> {
      m.device = messageRecipient;
      m.messageContent = cannedResponseContent;
      m.isDirectionToVehicle = true;
    });

    var cannedId = api.callAdd(TextMessageEntity, textMessageWithResponses).orElseThrow();
    out.format("Canned TextMessage added with id %s%n", cannedId.getId());
    textMessageWithResponses.setId(cannedId);

    var lastKnownSentDate = nowUtc;

    // -------
    // START: MOCK A DEVICE REPLY.
    // **FOR EXAMPLE PURPOSES ONLY.**
    // THIS LOGIC IS HANDLED BY THE MYGEOTAB SYSTEM.
    // YOU WOULD NOT NORMALLY DO THIS IN A WORKING ENVIRONMENT.
    // -------

    var textMessageFromDevice = apply(new TextMessage(), m -> {
      m.sent = nowUtc;
      m.delivered = nowUtc;
      m.user = sender;
      m.device = messageRecipient;
      m.messageContent = apply(new TextContent(), c -> {
        c.message = cannedResponseContent.cannedResponseOptions.getFirst().text;
        c.urgent = false;
      });
      m.parentMessage = textMessageWithResponses;
      m.isDirectionToVehicle = true;
    });

    var replyId = api.callAdd(TextMessageEntity, textMessageFromDevice).orElseThrow();
    out.format("Response TextMessage added with id %s%n", replyId.getId());

    // -------
    // END: MOCK A DEVICE REPLY
    // -------

    var textMessages = api
      .callGet(TextMessageEntity, apply(new TextMessageSearch(), s -> s.modifiedSinceDate = lastKnownSentDate), null)
      .orElse(List.of());
    out.format("%s TextMessages delivered/sent/read%n", textMessages.size());
  }

  private static void sendMessageWithGpsLocation(Api api, User sender, Device messageRecipient) {
    var lastKnownSentDate = Instant.now();

    /*
     * Location Message
     * A message with a GPS location. A series can be sent to comprise a route.
     * A clear message clears any previous location messages.
     */

    // Send a "Clear Previous Stops" message
    var clearMessage = apply(new TextMessage(), m -> {
      m.device = messageRecipient;
      m.user = sender;
      m.messageContent = apply(new LocationContent(), c -> {
        c.message = "Testing: Geotab API example clear all stops message";
        c.address = "Reset Stops";
        c.latitude = 0d;
        c.longitude = 0d;
      });
      m.isDirectionToVehicle = true;
    });

    var clearId = api.callAdd(TextMessageEntity, clearMessage).orElseThrow();
    out.format("Clear Stop TextMessage added with id %s%n", clearId.getId());

    // Send a location message
    var locationMessage = apply(new TextMessage(), m -> {
      m.device = messageRecipient;
      m.user = sender;
      m.messageContent = apply(new LocationContent(), c -> {
        c.message = "Testing: Geotab API example location message";
        c.address = "Geotab";
        c.latitude = 43.452879d;
        c.longitude = -79.701648d;
      });
      m.isDirectionToVehicle = true;
    });

    var locationId = api.callAdd(TextMessageEntity, locationMessage).orElseThrow();
    out.format("Location TextMessage added with id %s%n", locationId.getId());

    var textMessages = api
      .callGet(TextMessageEntity, apply(new TextMessageSearch(), s -> s.modifiedSinceDate = lastKnownSentDate), null)
      .orElse(List.of());
    out.format("%s TextMessages delivered/sent/read%n", textMessages.size());
  }
}
