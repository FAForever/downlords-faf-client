package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class AbstractFilterNodeController<U, N extends Observable, T> extends NodeController<Node> {

  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(item -> true);

  public abstract boolean hasDefaultValue();

  public abstract void resetFilter();

  public abstract N valueProperty();

  protected abstract U getValue();

  public void registerListener(BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addAndTriggerListener(valueProperty(), observable -> predicate.set(item -> filter.apply(getValue(), item)));
  }

  public ObjectProperty<Predicate<T>> predicateProperty() {
    return predicate;
  }

  public Predicate<T> getPredicate() {
    return predicate.get();
  }
}
