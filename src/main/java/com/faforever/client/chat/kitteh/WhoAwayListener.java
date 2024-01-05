package com.faforever.client.chat.kitteh;

import net.engio.mbassy.listener.Handler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.listener.AbstractDefaultListenerBase;
import org.kitteh.irc.client.library.event.client.ClientReceiveNumericEvent;
import org.kitteh.irc.client.library.event.helper.ClientEvent;
import org.kitteh.irc.client.library.feature.filter.NumericFilter;

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

  public record WhoAwayMessageEvent(
      Client client, String channel, String userName, boolean isAway
  ) implements ClientEvent {
    @Override
    public @NonNull Client getClient() {
      return client;
    }
  }

}