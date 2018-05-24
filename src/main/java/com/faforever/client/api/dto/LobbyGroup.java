package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Deprecated
@NoArgsConstructor
@AllArgsConstructor
@Type("lobbyGroup")
public class LobbyGroup {
  @Id
  private String userId;
  private LegacyAccessLevel accessLevel;
}