package com.faforever.client.chat.kitteh.listener;

import net.engio.mbassy.listener.Handler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.listener.AbstractDefaultListenerBase;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.client.ClientReceiveNumericEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.helper.ClientEvent;
import org.kitteh.irc.client.library.feature.filter.NumericFilter;

import java.util.Optional;

// This who listener only sends events marking users as away for a channel
public class WhoAwayListener extends AbstractDefaultListenerBase {

  /**
   * Constructs the listener.
   *
   * @param client client
   */
  public WhoAwayListener(Client.WithManagement client) {
    super(client);
  }

  @NumericFilter(352) // WHO
  @NumericFilter(354) // WHOX
  @Handler(priority = Integer.MAX_VALUE - 1)
  public void who(ClientReceiveNumericEvent event) {
    if (event.getParameters().size() < ((event.getNumeric() == 352) ? 8 : 9)) {
      this.trackException(event, "WHO response too short");
      return;
    }
    final String channel = event.getParameters().get(1);

    final String nick = event.getParameters().get(5);
    final String status = event.getParameters().get(6);
    boolean isAway = status.contains("G");

    this.fire(new WhoAwayMessageEvent(this.getClient(), channel, nick, isAway));
  }

  @NumericFilter(315) // WHO completed
  @Handler(priority = Integer.MAX_VALUE - 1)
  public void whoComplete(ClientReceiveNumericEvent event) {
    if (event.getParameters().size() < 2) {
      this.trackException(event, "WHO response too short");
      return;
    }
    Optional<Channel> whoChannel = this.getTracker().getChannel(event.getParameters().get(1));
    whoChannel.ifPresent(channel -> {
      this.fire(new WhoComplete(this.getClient(), channel));
    }); // No else, server might send other WHO information about non-channels.
  }

  public record WhoComplete(Client client, Channel channel) implements ChannelEvent, ClientEvent {
    @Override
    public @NonNull Client getClient() {
      return client;
    }

    @Override
    public @NonNull Channel getChannel() {
      return channel;
    }
  }

  public record WhoAwayMessageEvent(
      Client client, String channel, String userName, boolean isAway
  ) implements ClientEvent {
    @Override
    public @NonNull Client getClient() {
      return client;
    }
  }

}