package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.faforever.client.legacy.io.QStreamWriter;
import com.google.gson.GsonBuilder;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.io.OutputStream;

public class ClientMessageSerializer extends JsonMessageSerializer<ClientMessage> {

  private String username;
  private StringProperty sessionIdProperty;

  /**
   * Creates a message serializer that does not append username and session ID to sent messages.
   */
  public ClientMessageSerializer() {

  }

  /**
   * Creates a message serializer that appends username and session ID to sent messages.
   *
   * @param sessionIdProperty the session ID property, so that this serializer can be initialized before the session ID
   * has been set, but it will still get it afterwards.
   */
  public ClientMessageSerializer(String username, StringProperty sessionIdProperty) {
    this.username = username;
    this.sessionIdProperty = sessionIdProperty;
  }

  @Override
  public void serialize(ClientMessage object, OutputStream outputStream) throws IOException {
    super.serialize(object, outputStream);
  }

  @Override
  protected void appendMore(QStreamWriter qStreamWriter) throws IOException {
    if (username != null) {
      qStreamWriter.append(username);
    }
    if (sessionIdProperty != null) {
      qStreamWriter.append(sessionIdProperty.get());
    }
  }

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GameAccess.class, new GameAccessTypeAdapter());
    gsonBuilder.registerTypeAdapter(GameState.class, new GameStateTypeAdapter());
    gsonBuilder.registerTypeAdapter(VictoryCondition.class, new VictoryConditionTypeAdapter());
  }
}
