package com.faforever.client.serialization;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class SimpleListPropertyInstantiator extends StdValueInstantiator {


  public SimpleListPropertyInstantiator(DeserializationConfig config, JavaType valueType) {
    super(config, valueType);
  }

  @Override
  public SimpleListProperty<?> createUsingDefault(DeserializationContext ctxt) {
    return new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  @Override
  public boolean canCreateUsingDefault() {
    return true;
  }

}
