package org.apache.maven.artifact.versioning;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ComparableVersionDeserializer implements JsonDeserializer<ComparableVersion> {
  public static final ComparableVersionDeserializer INSTANCE = new ComparableVersionDeserializer();

  @Override
  public ComparableVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return new ComparableVersion(json.getAsJsonObject().get("value").getAsString());
  }
}
