package com.faforever.client.chat.jan;

import java.util.Set;

/**
 * Implemented by an external / remote service to define channels that should be joined,
 * for example an IRC chat implementation my have its own sever-side channel subscriptions.
 */
public interface ExternalChannelConfiguration {
  String getDefaultChannelName();
  Set<String> getChannelSubscriptions();
}
