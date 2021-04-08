package com.faforever.client.remote;

import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.io.QDataWriter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.serializer.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
public class JsonMessageSerializer<T extends SerializableMessage> implements Serializer<T> {

  private static final String CONFIDENTIAL_INFORMATION_MASK = "********";

  private Gson gson;

  // TODO Clean this up, such that the message is logged within ServerWriter and everything makes much more sense
  @Override
  public void serialize(SerializableMessage message, OutputStream outputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Writer jsonStringWriter = new StringWriter();

    // Serialize the object into a StringWriter which is later send as one string block with its size prepended.
    getGson().toJson(message, message.getClass(), fixedJsonWriter(jsonStringWriter));

    QDataWriter qDataWriter = new QDataWriter(byteArrayOutputStream);
    qDataWriter.append(jsonStringWriter.toString());

    byte[] byteArray = byteArrayOutputStream.toByteArray();

    if (log.isDebugEnabled()) {
      // Remove the first 4 bytes which contain the length of the following data
      String data = new String(Arrays.copyOfRange(byteArray, 4, byteArray.length), StandardCharsets.UTF_16BE);

      for (String stringToMask : message.getStringsToMask()) {
        data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"");
      }

      log.debug("Writing to server: {}", data);
    }

    outputStream.write(byteArray);
  }

  private Gson getGson() {
    if (gson == null) {
      GsonBuilder gsonBuilder = new GsonBuilder()
          .disableHtmlEscaping()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

      addTypeAdapters(gsonBuilder);

      gson = gsonBuilder.create();
    }
    return gson;
  }

  private JsonWriter fixedJsonWriter(Writer writer) {
    // Does GSON suck because its separator can't be set, or python because it can't handle JSON without a space after colon?
    try {
      JsonWriter jsonWriter = new JsonWriter(writer);
      jsonWriter.setSerializeNulls(false);

      Field separatorField = JsonWriter.class.getDeclaredField("separator");
      separatorField.setAccessible(true);
      separatorField.set(jsonWriter, ": ");

      return jsonWriter;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Allows subclasses to register additional type adapters. Super doesn't need to be called.
   */
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    // To be overridden by subclasses, if desired
  }
}
