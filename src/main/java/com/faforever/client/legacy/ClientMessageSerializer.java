package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.writer.JsonSerializer;
import com.faforever.client.legacy.writer.QStreamWriter;
import com.google.gson.GsonBuilder;
import javafx.beans.property.StringProperty;

import java.io.IOException;

public class ClientMessageSerializer extends JsonSerializer<ClientMessage> {

  private String username;
  private StringProperty sessionIdProperty;

  /**
   * @param sessionIdProperty the session ID property, so that this serializer can be initialized before the
   * session ID has been set, but it will still get it afterwards.
   */
  public ClientMessageSerializer(String username, StringProperty sessionIdProperty) {
    this.username = username;
    this.sessionIdProperty = sessionIdProperty;
  }

  @Override
  protected void appendMore(QStreamWriter qStreamWriter) throws IOException {
    qStreamWriter.append(username);
    qStreamWriter.append(sessionIdProperty.get());
  }

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GameAccess.class, new GameAccessTypeAdapter());
  }
}
