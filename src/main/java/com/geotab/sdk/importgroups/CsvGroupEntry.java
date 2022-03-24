package com.geotab.sdk.importgroups;

import com.geotab.model.entity.group.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models a row from a file with {@link Group} data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvGroupEntry {

  private String parentGroupName;

  private String groupName;
}
