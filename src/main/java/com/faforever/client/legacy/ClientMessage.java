package com.faforever.client.legacy;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

public class ClientMessage implements Serializable {

  public static ClientMessage login(String username, String password, String session) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "hello";
    clientMessage.login = username;
    clientMessage.password = password;
    clientMessage.session = session;
    return clientMessage;
  }

  public static ClientMessage askSession(String username) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "ask_session";
    return clientMessage;
  }

  private Integer version;
  private String command;
  private String login;
  private String password;
  private String uniqueId;
  private String localIp;
  private String session;

  public void serialize(QStreamWriter writer, String username, String session) throws IOException {
    Writer stringWriter = new StringWriter();
    JsonWriter jsonWriter = fixedJsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("command");
    jsonWriter.value(command);
    jsonWriter.endObject();

    writer.append(stringWriter.toString());
    writer.append(username);
    writer.append(session);
  }

  private JsonWriter fixedJsonWriter(Writer out) {
    // Does GSON suck because its separator can't be set, or python because it can't handle JSON without a space after colon?
    try {
      JsonWriter jsonWriter = new JsonWriter(out);
      Field separatorField = JsonWriter.class.getDeclaredField("separator");
      separatorField.setAccessible(true);
      separatorField.set(jsonWriter, ": ");

      return jsonWriter;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
