package com.faforever.client.ui.transitions;

import com.jfoenix.transitions.CacheMemento;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.Arrays;

/** Ported from JFoenix since we wanted to get rid of the JFoenix dependency */
public class CachedTransition extends Transition {
  protected final Node node;
  protected final ObjectProperty<Timeline> timeline = new SimpleObjectProperty<>();

  private final CacheMemento[] mementos;

  public CachedTransition(final Node node, final Timeline timeline) {
    this.node = node;
    this.timeline.set(timeline);
    mementos = node == null ? new CacheMemento[0] : new CacheMemento[]{new CacheMemento(node)};
    statusProperty().addListener(observable -> {
      if (getStatus() == Status.RUNNING) {
        starting();
      } else {
        stopping();
      }
    });
  }

  public CachedTransition(final Node node, final Timeline timeline, CacheMemento... cacheMomentos) {
    this.node = node;
    this.timeline.set(timeline);
    mementos = new CacheMemento[(node == null ? 0 : 1) + cacheMomentos.length];
    if (node != null) {
      mementos[0] = new CacheMemento(node);
    }
    System.arraycopy(cacheMomentos, 0, mementos, node == null ? 0 : 1, cacheMomentos.length);
    statusProperty().addListener(observable -> {
      if (getStatus() == Status.RUNNING) {
        starting();
      } else {
        stopping();
      }
    });
  }

  protected void starting() {
    Arrays.stream(mementos).forEach(CacheMemento::cache);
  }

  protected void stopping() {
    Arrays.stream(mementos).forEach(CacheMemento::restore);
  }

  @Override
  protected void interpolate(double d) {
    timeline.get().playFrom(Duration.seconds(d));
    timeline.get().stop();
  }
}
