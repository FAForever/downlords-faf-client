package com.faforever.client.vault;

import com.faforever.client.fx.NodeController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

public abstract class VaultEntityCardController<T> extends NodeController<Node> {

  protected final ObjectProperty<T> entity = new SimpleObjectProperty<>();

  public T getEntity() {
    return entity.get();
  }

  public ObjectProperty<T> entityProperty() {
    return entity;
  }

  public void setEntity(T entity) {
    this.entity.set(entity);
  }

  @Override
  public abstract Node getRoot();
}
