package com.faforever.client.serialization;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

public class SimpleMapPropertyInstantiator extends StdValueInstantiator {


  public SimpleMapPropertyInstantiator(DeserializationConfig config, JavaType valueType) {
    super(config, valueType);
  }

  @Override
  public SimpleMapProperty<?, ?> createUsingDefault(DeserializationContext ctxt) {
    return new SimpleMapProperty<>(FXCollections.observableHashMap());
  }

  @Override
  public boolean canCreateUsingDefault() {
    return true;
  }

}
