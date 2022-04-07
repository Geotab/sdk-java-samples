package com.geotab.sdk.importdevices;

import com.geotab.model.entity.device.Device;

/**
 * Models a row from a file with {@link Device} data.
 */
public class CsvDeviceEntry {

  public String description;

  public String nodeName;

  public String serialNumber;

  public String vin;
}
