package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.fa.relay.ice.event.GpgGameMessageEvent;
import com.faforever.client.fa.relay.ice.event.IceAdapterStateChanged;
import com.faforever.client.remote.FafService;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

/**
 * Dispatches all methods that the ICE adapter can call on its client.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class IceAdapterCallbacks {

  private final EventBus eventBus;
  private final FafService fafService;

  @Inject
  public IceAdapterCallbacks(EventBus eventBus, FafService fafService) {
    this.eventBus = eventBus;
    this.fafService = fafService;
  }

  public void onConnectionStateChanged(String newState) {
    log.debug("ICE adapter connection state changed to: {}", newState);
    eventBus.post(new IceAdapterStateChanged(newState));
  }

  public void onGpgNetMessageReceived(String header, List<Object> chunks) {
    log.debug("Message from game: '{}' '{}'", header, chunks);
    eventBus.post(new GpgGameMessageEvent(new GpgGameMessage(header, chunks)));
  }

  public void onIceMsg(long localPlayerId, long remotePlayerId, Object message) {
    log.debug("ICE message for connection '{}/{}': {}", localPlayerId, remotePlayerId, message);
    fafService.sendIceMessage((int) remotePlayerId, message);
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
