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
      Type rawType = ((ParameterizedType) typeOfT).getRawType();

      if (rawType == ObjectProperty.class) {
        return new SimpleObjectProperty<>(context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]));
      } else if (rawType == ListProperty.class) {
        return new SimpleListProperty<>(FXCollections.observableList(context.deserialize(json, List.class)));
      } else if (rawType == SetProperty.class) {
        // Why is this the only call that needs paramaterization?
        return new SimpleSetProperty<>(FXCollections.observableSet(context.<Set<Object>>deserialize(json, Set.class)));
      } else if (rawType == MapProperty.class) {
        return new SimpleMapProperty<>(FXCollections.observableMap(context.deserialize(json, Map.class)));
      }
    }

    throw new IllegalStateException("Unhandled object type: " + typeOfT);
  }
}
