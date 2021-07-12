package com.faforever.client.remote.domain.outbound.gpg;



import java.util.List;

public class IceMessage extends GpgOutboundMessage {
  public static final String COMMAND = "IceMsg";


  public IceMessage(int remotePlayerId, Object message) {
    super(COMMAND, List.of(remotePlayerId, message));
  }
}
