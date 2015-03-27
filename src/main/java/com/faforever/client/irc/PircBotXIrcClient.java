package com.faforever.client.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PircBotXIrcClient implements IrcClient {

  private Map<Class<? extends Event>, ArrayList<IrcEventListener>> eventListeners;

  public PircBotXIrcClient() {
    eventListeners = new HashMap<>();
  }

  @Override
  public void connect(PircBotX pircBotX) throws IOException {
    try {
      pircBotX.startBot();
    } catch (IrcException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addEventListener(Class<? extends Event> eventClass, IrcEventListener listener) {
    if (!eventListeners.containsKey(eventClass)) {
      eventListeners.put(eventClass, new ArrayList<IrcEventListener>());
    }
  }

  @Override
  public void onEvent(Event event) throws Exception {
    if (!eventListeners.containsKey(event)) {
      return;
    }

    for (IrcEventListener listener : eventListeners.get(event)) {
      listener.onEvent(event);
    }
  }
}
