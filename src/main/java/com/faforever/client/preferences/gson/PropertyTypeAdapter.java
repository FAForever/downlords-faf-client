package com.faforever.client.preferences.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableObjectValue;
import javafx.collections.FXCollections;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyTypeAdapter implements JsonSerializer<Property>, JsonDeserializer<Property> {

  private class CustomType implements ParameterizedType {

    private final Class<?> rawType;
    private final Type[] typeArguments;

    public CustomType(Class<?> rawType, Type[] typeArguments) {
      this.rawType = rawType;
      this.typeArguments = typeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return typeArguments;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public Type getOwnerType() {
      return null;
    }
  }

  @Override
  public JsonElement serialize(Property src, Type typeOfSrc, JsonSerializationContext context) {
    if (src.getValue() == null) {
      return JsonNull.INSTANCE;
    }
    if (src instanceof StringProperty) {
      return new JsonPrimitive(((StringProperty) src).get());
    }
    if (src instanceof IntegerProperty) {
      return new JsonPrimitive(((IntegerProperty) src).get());
    }
    if (src instanceof DoubleProperty) {
      return new JsonPrimitive(((DoubleProperty) src).get());
    }
    if (src instanceof LongProperty) {
      return new JsonPrimitive(((LongProperty) src).get());
    }
    if (src instanceof FloatProperty) {
      return new JsonPrimitive(((FloatProperty) src).get());
    }
    if (src instanceof BooleanProperty) {
      return new JsonPrimitive(((BooleanProperty) src).get());
    }
    if (src instanceof WritableObjectValue) {
      return context.serialize(((WritableObjectValue) src).get());
    }

    throw new IllegalStateException("Unhandled object type: " + src.getClass());
  }

  @Override
  public Property deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (typeOfT == StringProperty.class) {
      return new SimpleStringProperty(json.getAsString());
    }
    if (typeOfT == IntegerProperty.class) {
      return new SimpleIntegerProperty(json.getAsInt());
    }
    if (typeOfT == DoubleProperty.class) {
      return new SimpleDoubleProperty(json.getAsDouble());
    }
    if (typeOfT == LongProperty.class) {
      return new SimpleLongProperty(json.getAsLong());
    }
    if (typeOfT == FloatProperty.class) {
      return new SimpleFloatProperty(json.getAsFloat());
    }
    if (typeOfT == BooleanProperty.class) {
      return new SimpleBooleanProperty(json.getAsBoolean());
    }
    if (typeOfT == SetProperty.class) {
      return new SimpleSetProperty<>(context.deserialize(json, Set.class));
    }
    if (typeOfT == MapProperty.class) {
      return new SimpleSetProperty<>(context.deserialize(json, Map.class));
    }
    if (typeOfT instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) typeOfT;
      Type rawType = parameterizedType.getRawType();

      if (rawType == ObjectProperty.class) {
        return new SimpleObjectProperty<>(context.deserialize(json, parameterizedType.getActualTypeArguments()[0]));
      } else if (rawType == ListProperty.class) {
        CustomType type = new CustomType(List.class, parameterizedType.getActualTypeArguments());
        return new SimpleListProperty<>(FXCollections.observableList(context.deserialize(json, type)));
      } else if (rawType == SetProperty.class) {
        CustomType type = new CustomType(Set.class, parameterizedType.getActualTypeArguments());
        // Why is this the only call that needs paramaterization?
        return new SimpleSetProperty<>(FXCollections.observableSet(context.<Set<Object>>deserialize(json, type)));
      } else if (rawType == MapProperty.class) {
        CustomType type = new CustomType(Map.class, parameterizedType.getActualTypeArguments());
        return new SimpleMapProperty<>(FXCollections.observableMap(context.deserialize(json, type)));
      }
    }

    throw new IllegalStateException("Unhandled object type: " + typeOfT);
  }
}
