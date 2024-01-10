package com.faforever.client.chat.kitteh;

import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.WithManagement;
import org.kitteh.irc.client.library.defaults.listener.AbstractDefaultListenerBase;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.client.ClientReceiveCommandEvent;
import org.kitteh.irc.client.library.feature.filter.CommandFilter;

import java.util.List;

/**
 * Default JOIN listener, producing events using default classes.
 */
public class RedactListener extends AbstractDefaultListenerBase {
  /**
   * Constructs the listener.
   *
   * @param client client
   */
  public RedactListener(Client.WithManagement client) {
    super(client);
  }

  @CommandFilter("REDACT")
  @Handler(priority = Integer.MAX_VALUE - 1)
  public void redact(ClientReceiveCommandEvent event) {
    List<String> parameters = event.getParameters();
    if (parameters.size() < 2) {
      this.trackException(event, "REDACT message too short");
      return;
    }
    if (!(event.getActor() instanceof User user)) {
      this.trackException(event, "Message from something other than a user");
      return;
    }
    MessageTargetInfo messageTargetInfo = this.getTypeByTarget(parameters.getFirst());
    ServerMessage source = event.getSource();
    WithManagement client = this.getClient();
    String redactedMessageId = parameters.get(1);
    String message;
    if (parameters.size() > 2) {
      message = parameters.get(2);
    } else {
      message = null;
    }
    if (messageTargetInfo instanceof MessageTargetInfo.Private) {
      this.fire(new PrivateRedactMessageEvent(client, source, user, parameters.getFirst(), message, redactedMessageId));
    } else if (messageTargetInfo instanceof MessageTargetInfo.ChannelInfo channelInfo) {
      this.fire(
          new ChannelRedactMessageEvent(client, source, user, channelInfo.getChannel(), message, redactedMessageId));
    } else if (messageTargetInfo instanceof MessageTargetInfo.TargetedChannel channelInfo) {
      this.fire(
          new ChannelTargetedRedactMessageEvent(client, source, user, channelInfo.getChannel(), channelInfo.getPrefix(),
                                                message, redactedMessageId));
    }
  }
}
