package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Message sent from the server to the client containing a list of ICE servers to use.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IceServersServerMessage extends FafServerMessage {

  @SerializedName("ice_servers")
  private List<IceServer> iceServers;

  @Data
  public static class IceServer {
    private String url;
    private String urls[];
    private String credential;
    private String credentialType;
    private String username;
  }
}
