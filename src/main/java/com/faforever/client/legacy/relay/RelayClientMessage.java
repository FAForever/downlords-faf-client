package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.ServerWritable;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.relay.RelayServerAction.PONG;

/**
 * Represents a domain that can be sent to the FAF relay server. The domain is serialized to JSON in a strict format
 * that is readable by the server.
 */
public class RelayClientMessage implements ServerWritable {

  private RelayServerAction action;
  // Because typos in protocols are cool (this class is JSON serialized).
  private List<Object> chuncks;

  public RelayClientMessage(RelayServerAction action, List<Object> chunks) {
    this.action = action;
    this.chuncks = chunks;
  }

  public List<Object> getChunks() {
    return chuncks;
  }

  public void write(Gson gson, Writer writer) throws IOException {
    gson.toJson(this, RelayClientMessage.class, fixedJsonWriter(writer));
  }

  @Override
  public List<String> getStringsToMask() {
    return Collections.emptyList();
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

  public RelayServerAction getAction() {
    return action;
  }

  public static RelayClientMessage pong() {
    return new RelayClientMessage(PONG, Collections.emptyList());
  }

}
