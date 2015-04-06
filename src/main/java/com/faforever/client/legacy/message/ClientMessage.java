package com.faforever.client.legacy.message;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Map;

public class ClientMessage implements ServerWritable {

  public static ClientMessage selectAvatar(String avatar) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.action = "select";
    clientMessage.command = "avatar";
    clientMessage.avatar = avatar;
    return clientMessage;
  }


  public static ClientMessage listAvatar() {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.action = "list_avatar";
    clientMessage.command = "avatar";
    return clientMessage;
  }

  public static ClientMessage login(String username, String password, String session, String uniqueId, String localIp, int version) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "hello";
    clientMessage.login = username;
    clientMessage.password = password;
    clientMessage.session = session;
    clientMessage.uniqueId = uniqueId;
    clientMessage.localIp = localIp;
    clientMessage.version = version;
    return clientMessage;
  }

  public static ClientMessage hostGame(GameAccess gameAccess, String mapname, String title, int port, Map<String, Boolean> options, String mod) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "game_host";
    clientMessage.access = gameAccess.getString();
    clientMessage.mapname = mapname;
    clientMessage.title = title;
    clientMessage.gameport = port;
    clientMessage.options = options;
    clientMessage.mod = mod;
    return clientMessage;
  }

  public static ClientMessage askSession(String username) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "ask_session";
    return clientMessage;
  }

  private String action;
  private String avatar;
  private Integer version;
  private String command;
  private String login;
  private String password;
  private String uniqueId;
  private String localIp;
  private String session;
  private String access;
  private String mapname;
  private String title;
  private Integer gameport;
  private Map<String, Boolean> options;
  private String mod;

  public void write(Gson gson, Writer writer) throws IOException {
    gson.toJson(this, ClientMessage.class, fixedJsonWriter(writer));
  }

  @Override
  public boolean isConfidential() {
    return password != null;
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
