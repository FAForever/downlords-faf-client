package com.faforever.client.notification;

import javafx.event.Event;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * Notifications have actions associated with it. This class represents such an action, which is usually displayed to
 * the user as a button.
 */
public class Action {

  @Getter
  private final String title;
  private final Consumer<Event> callback;
  @Getter
  private final Type type;

  public Action(Consumer<Event> callback) {
    this(null, Type.OK_DONE, callback);
  }

  /**
   * Creates an action that calls the specified callback when executed. Also, a type is specified that
   */
  public Action(String title, Type type, Consumer<Event> callback) {
    this.title = title;
    this.type = type;
    this.callback = callback;
  }

  /**
   * Creates an action that does nothing.
   */
  public Action(String title) {
    this(title, Type.OK_DONE, null);
  }

  /**
   * Creates an action that calls the specified callback when executed. The action will have the default action type
   * {@link com.faforever.client.notification.Action.Type#OK_DONE}.
   */
  public Action(String title, Consumer<Event> callback) {
    this(title, Type.OK_DONE, callback);
  }

  /**
   * Calls the specified callback, if any. Subclasses may override.
   */
  public void call(Event event) {
    if (callback != null) {
      callback.accept(event);
    }
  }

  public enum Type {
    OK_DONE,
    OK_STAY
  }
}
