package com.faforever.client.legacy.writer;

import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.legacy.io.QStreamWriter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class JsonMessageSerializer<T extends SerializableMessage> implements Serializer<T> {

  public static final String CONFIDENTIAL_INFORMATION_MASK = "********";

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Gson gson;

  // TODO Clean this up, such that the message is logged within ServerWriter and everything makes much more sense
  @Override
  public void serialize(SerializableMessage message, OutputStream outputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    Writer jsonStringWriter = new StringWriter();

    // Serialize the object into a StringWriter which is later send as one string block with its size prepended.
    getGson().toJson(message, message.getClass(), fixedJsonWriter(jsonStringWriter));

    QStreamWriter qStreamWriter = new QStreamWriter(byteArrayOutputStream);
    qStreamWriter.append(jsonStringWriter.toString());

    appendMore(qStreamWriter);

    byte[] byteArray = byteArrayOutputStream.toByteArray();

    if (logger.isDebugEnabled()) {
      // Remove the first 4 bytes which contain the length of the following data
      String data = new String(Arrays.copyOfRange(byteArray, 4, byteArray.length), StandardCharsets.UTF_16BE);

      for (String stringToMask : message.getStringsToMask()) {
        data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"");
      }

      logger.debug("Writing to server: {}", data);
    }

    outputStream.write(byteArray);
  }

  /**
   * Allows subclasses to append more stuff after the serialized JSON. Default implementation does nothing, so super
   * doesn't need to be called.
   */
  protected void appendMore(QStreamWriter qStreamWriter) throws IOException {
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

  /**
   * Allows subclasses to register additional type adapters. Super doesn't need to be called.
   */
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {

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
}
