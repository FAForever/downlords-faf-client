package com.faforever.client.filter;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class AbstractFilterNodeController<U, N extends ObservableValue<U>, T> implements Controller<Node> {

  @Getter
  @Setter
  private FilterName filterName;

  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(item -> true);

  public abstract boolean hasDefaultValue();

  public abstract void resetFilter();

  public abstract N getObservable();

  public void registerListener(BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addAndTriggerListener(getObservable(), observable -> predicate.set(item -> filter.apply(getObservable().getValue(), item)));
  }

  public ObjectProperty<Predicate<T>> getPredicateProperty() {
    return predicate;
  }

  public Predicate<T> getPredicate() {
    return predicate.get();
  }

  public void bindBidirectional(Property<?> property) {
    // // To be overridden by subclass
  }
}
