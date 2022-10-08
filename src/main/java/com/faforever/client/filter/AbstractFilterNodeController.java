package com.faforever.client.filter;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class AbstractFilterNodeController<U, N extends Observable, T> implements Controller<Node> {

  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(item -> true);

  public abstract boolean hasDefaultValue();

  public abstract void resetFilter();

  public abstract N getObservable();

  protected abstract U getValue();

  public void registerListener(BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addAndTriggerListener(getObservable(), observable -> predicate.set(item -> filter.apply(getValue(), item)));
  }

  public ObjectProperty<Predicate<T>> getPredicateProperty() {
    return predicate;
  }

  public Predicate<T> getPredicate() {
    return predicate.get();
  }
}
