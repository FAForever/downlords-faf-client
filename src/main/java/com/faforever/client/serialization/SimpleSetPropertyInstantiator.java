package com.faforever.client.serialization;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public class SimpleSetPropertyInstantiator extends StdValueInstantiator {


  public SimpleSetPropertyInstantiator(DeserializationConfig config, JavaType valueType) {
    super(config, valueType);
  }

  @Override
  public SimpleSetProperty<?> createUsingDefault(DeserializationContext ctxt) {
    return new SimpleSetProperty<>(FXCollections.observableSet());
  }

  @Override
  public boolean canCreateUsingDefault() {
    return true;
  }

}
