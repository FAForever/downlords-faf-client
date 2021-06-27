package com.faforever.client.remote.domain.inbound.faf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * Message sent from the server to the client containing a list of ICE servers to use.
 */

@Value
@EqualsAndHashCode(callSuper = true)
public class IceServersMessage extends FafInboundMessage {
  public static final String COMMAND = "ice_servers";

  @JsonProperty("ice_servers")
  List<IceServer> iceServers;

  @Value
  public static class IceServer {
    String url;
    String[] urls;
    String credential;
    String credentialType;
    String username;
  }
}
