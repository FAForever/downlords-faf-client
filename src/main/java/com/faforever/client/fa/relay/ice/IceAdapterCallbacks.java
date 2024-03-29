package com.faforever.client.fa.relay.ice;


import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.MessageTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dispatches all methods that the ICE adapter can call on its client.
 */
@SuppressWarnings("unused")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class IceAdapterCallbacks {

  private final IceAdapter iceAdapter;
  private final FafServerAccessor fafServerAccessor;

  public void onConnectionStateChanged(String newState) {
    log.debug("ICE adapter connection state changed to: {}", newState);
    iceAdapter.onIceAdapterStateChanged(newState);
  }

  public void onGpgNetMessageReceived(String header, List<Object> chunks) {
    log.debug("Message from game: '{}' '{}'", header, chunks);
    iceAdapter.onGpgGameMessage(new GpgGameOutboundMessage(header, chunks, MessageTarget.GAME));
  }

  public void onIceMsg(long localPlayerId, long remotePlayerId, Object message) {
    log.debug("ICE message for connection '{}/{}': {}", localPlayerId, remotePlayerId, message);
    fafServerAccessor.sendIceMessage((int) remotePlayerId, message);
  }

  public void onIceConnectionStateChanged(long localPlayerId, long remotePlayerId, String state) {
    log.debug("ICE connection state for peer '{}' changed to: {}", remotePlayerId, state);
  }

  public void onConnected(long localPlayerId, long remotePlayerId, boolean connected) {
    if (connected) {
      log.debug("Connection between '{}' and '{}' has been established", localPlayerId, remotePlayerId);
    } else {
      log.debug("Connection between '{}' and '{}' has been lost", localPlayerId, remotePlayerId);
    }
  }
}
