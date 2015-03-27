package com.faforever.client.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;

import java.io.IOException;

public interface IrcClient extends Listener<PircBotX> {

  public interface IrcEventListener<T extends Event> {

    void onEvent(T event);
  }

  void connect(PircBotX pircBotX) throws IOException;

  void addEventListener(Class<? extends Event> eventClass, IrcEventListener listener);
}
