package com.faforever.client.legacy;

import java.util.List;

/**
 * @see #onJoinChannelsRequest(List)
 */
public interface OnJoinChannelsRequestListener {

  /**
   * Called when the server sends a list of channels to be joined.
   */
  void onJoinChannelsRequest(List<String> channelNames);
}
