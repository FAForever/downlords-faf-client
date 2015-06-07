package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.writer.JsonSerializer;
import com.faforever.client.legacy.writer.QStreamWriter;
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
    qStreamWriter.appendWithSize(username);
    qStreamWriter.appendWithSize(sessionIdProperty.get());
  }
}
