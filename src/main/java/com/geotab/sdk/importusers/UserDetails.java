package com.geotab.sdk.importusers;

import com.geotab.model.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDetails {

  private String organizationNodeNames;

  private String securityNodeName;

  private User user;
}
