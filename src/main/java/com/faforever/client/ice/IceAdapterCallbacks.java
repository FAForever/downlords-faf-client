package com.faforever.client.ice;

import com.faforever.client.ice.event.GpgGameMessageEvent;
import com.faforever.client.relay.GpgGameMessage;
import com.faforever.client.remote.FafService;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Dispatches all methods that the ICE adapter can call on its client.
 */
public class IceAdapterCallbacks {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  EventBus eventBus;

  @Resource
  FafService fafService;

  public void onNeedSdp(int localPlayerId, int remotePlayerId) {
    logger.debug("SDP for connection {}/{} requested", localPlayerId, remotePlayerId);
  }

  public void onConnectionStateChanged(String newState) {
    logger.debug("ICE adapter connection state changed: {}", newState);
  }

  public void onGpgNetMessageReceived(String header, List<Object> chunks) {
    logger.debug("Message from game: {} {}", header, chunks);
    eventBus.post(new GpgGameMessageEvent(new GpgGameMessage(header, chunks)));
  }

  public void onGatheredSdp(int localPlayerId, int remotePlayerId, String sdp) {
    logger.debug("Gathered SDP for connection {}/{}: {}", localPlayerId, remotePlayerId, sdp);
    fafService.sendSdp(localPlayerId, remotePlayerId, sdp);
  }

  public void onIceStateChanged(int localPlayerId, int remotePlayerId, String state) {
    logger.debug("Connection state for peer {} changed: {}", remotePlayerId, state);
  }
}
