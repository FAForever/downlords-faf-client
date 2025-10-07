package com.faforever.client.chat.kitteh.network;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.feature.network.JavaResolver;
import org.kitteh.irc.client.library.feature.network.NetworkHandler;
import org.kitteh.irc.client.library.feature.network.Resolver;
import org.kitteh.irc.client.library.feature.sts.StsClientState;
import org.kitteh.irc.client.library.feature.sts.StsMachine;
import org.kitteh.irc.client.library.feature.sts.StsPolicy;
import org.kitteh.irc.client.library.util.Sanity;
import org.kitteh.irc.client.library.util.ToStringer;

import java.util.Optional;

public class WebsocketNetworkHandler implements NetworkHandler {
  private static final WebsocketNetworkHandler instance = new WebsocketNetworkHandler();

  /**
   * Gets the single instance of this class.
   *
   * @return instance
   */
  public static @NonNull WebsocketNetworkHandler getInstance() {
    return WebsocketNetworkHandler.instance;
  }

  private Resolver resolver = new JavaResolver();

  private WebsocketNetworkHandler() {
    // NOOP
  }

  @Override
  public @NonNull WebSocketConnection connect(Client.@NonNull WithManagement client) {
    // STS Override
    if (client.getStsMachine().isPresent() && !client.isSecureConnection()) {
      String hostname = client.getServerAddress().getHost();
      final StsMachine machine = client.getStsMachine().get();
      Optional<StsPolicy> policy = machine.getStorageManager().getEntry(hostname);
      if (policy.isPresent()) {
        machine.setStsPolicy(policy.get());
        machine.setCurrentState(StsClientState.STS_POLICY_CACHED);
      }
    }

    return new WebSocketConnection(client);
  }

  @Override
  public @NonNull Resolver getResolver() {
    return this.resolver;
  }

  @Override
  public void setResolver(@NonNull Resolver resolver) {
    this.resolver = Sanity.nullCheck(resolver, "Resolver");
  }

  @Override
  public @NonNull String toString() {
    return new ToStringer(this).toString();
  }
}
