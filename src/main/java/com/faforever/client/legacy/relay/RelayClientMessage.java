package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.message.ServerWritable;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message that can be sent to the FAF relay server. The message is serialized to JSON in a strict format
 * that is readable by the server.
 */
public class RelayClientMessage implements ServerWritable {

  public static RelayClientMessage pong() {
    return create("pong", Collections.emptyList());
  }

  public static RelayClientMessage create(String action, List<Object> chunks) {
    RelayClientMessage relayClientMessage = new RelayClientMessage();
    relayClientMessage.action = action;
    relayClientMessage.chuncks = chunks;
    return relayClientMessage;
  }

  private String action;

  // Because typos in protocols are cool
  private List<Object> chuncks;

  private RelayClientMessage() {
    // Private
  }

  public void write(Gson gson, Writer writer) throws IOException {
    gson.toJson(this, RelayClientMessage.class, fixedJsonWriter(writer));
  }

  @Override
  public boolean isConfidential() {
    return false;
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
