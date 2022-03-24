package com.geotab.sdk.importdevices;

import com.geotab.model.entity.device.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models a row from a file with {@link Device} data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvDeviceEntry {

  private String description;

  private String nodeName;

  private String serialNumber;

  private String vin;
}
