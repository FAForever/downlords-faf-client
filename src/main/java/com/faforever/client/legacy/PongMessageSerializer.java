package com.faforever.client.legacy;

import com.faforever.client.legacy.io.QDataWriter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import org.springframework.core.serializer.Serializer;

import java.io.IOException;
import java.io.OutputStream;

public class PongMessageSerializer implements Serializer<PongMessage> {

  private final StringProperty username;
  private final ObjectProperty<Long> sessionId;

  /**
   * @param sessionId the session ID property, so that this serializer can be initialized before the session ID has been
   * set, but it will still get it afterwards.
   */
  public PongMessageSerializer(StringProperty username, ObjectProperty<Long> sessionId) {
    this.username = username;
    this.sessionId = sessionId;
  }

  @Override
  public void serialize(PongMessage object, OutputStream outputStream) throws IOException {
    QDataWriter qDataWriter = new QDataWriter(outputStream);
    qDataWriter.append(object.getString());
    if (username.get() != null) {
      qDataWriter.append(username.get());
    }
    if (sessionId.get() != null) {
      qDataWriter.append(String.valueOf(sessionId.get()));
    }
    qDataWriter.flush();
  }
}
