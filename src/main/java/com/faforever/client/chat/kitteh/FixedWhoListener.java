package com.faforever.client.chat.kitteh;

import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.listener.AbstractDefaultListenerBase;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveNumericEvent;
import org.kitteh.irc.client.library.feature.filter.NumericFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The Default Who Listener ignores the first status char for some reason
public class FixedWhoListener extends AbstractDefaultListenerBase {
  private final List<ServerMessage> whoMessages = new ArrayList<>();

  /**
   * Constructs the listener.
   *
   * @param client client
   */
  public FixedWhoListener(Client.WithManagement client) {
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
    final Optional<Channel> channel = this.getTracker().getChannel(event.getParameters().get(1));
    channel.ifPresent(ch -> {
      final String ident = event.getParameters().get(2);
      final String host = event.getParameters().get(3);
      final String server = event.getParameters().get(4);
      final String nick = event.getParameters().get(5);
      final User user = (User) this.getTracker().getActor(nick + '!' + ident + '@' + host);
      this.getTracker().trackUser(user);
      this.getTracker().setUserServer(nick, server);
      final String status = event.getParameters().get(6);
      String realName;
      switch (event.getNumeric()) {
        case 352:
          realName = event.getParameters().get(7);
          break;
        case 354:
        default:
          String account = event.getParameters().get(7);
          this.getTracker().setUserAccount(nick, "0".equals(account) ? null : account);
          realName = event.getParameters().get(8);
          break;
      }
      this.getTracker().setUserRealName(nick, realName);
      final Set<ChannelUserMode> modes = new HashSet<>();
      for (char prefix : status.toCharArray()) {
        if (prefix == 'G') {
          this.getTracker().setUserAway(nick, true);
          continue;
        }
        if (prefix == '*') {
          this.getTracker().setUserOperString(nick, "*");
          continue;
        }
        for (ChannelUserMode mode : this.getClient().getServerInfo().getChannelUserModes()) {
          if (mode.getNickPrefix() == prefix) {
            modes.add(mode);
            break;
          }
        }
      }
      this.getTracker().trackChannelUser(ch.getName(), user, modes);
      this.whoMessages.add(event.getServerMessage());
    }); // No else, server might send other WHO information about non-channels.
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
      this.getTracker().setChannelListReceived(channel.getName());
      this.whoMessages.add(event.getServerMessage());
      this.fire(new ChannelUsersUpdatedEvent(this.getClient(), this.whoMessages, channel));
      this.whoMessages.clear();
    }); // No else, server might send other WHO information about non-channels.
  }
}