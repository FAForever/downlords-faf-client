package com.faforever.client.remote;

import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.io.QDataWriter;
import com.faforever.client.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.serializer.Serializer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends data to the server. Classes should not use the server writer directly, but e.g. {@link
 * com.faforever.client.remote.FafService} or any other server accessor instead.
 */
@Slf4j
public class ServerWriter implements Closeable {

  private final QDataWriter qDataWriter;
  private final Map<Class<?>, Serializer<?>> objectWriters;

  public ServerWriter(OutputStream outputStream) {
    qDataWriter = new QDataWriter(new DataOutputStream(new BufferedOutputStream(outputStream)));
    objectWriters = new HashMap<>();
  }

  public void registerMessageSerializer(Serializer<?> objectSerializer, Class<?> writableClass) {
    objectWriters.put(writableClass, objectSerializer);
  }

  @SuppressWarnings("unchecked")
  public void write(SerializableMessage object) {
    Class<?> clazz = object.getClass();

    Serializer<SerializableMessage> serializer = (Serializer<SerializableMessage>) findSerializerForClass(clazz);

    Assert.checkNullIllegalState(serializer, () -> "No object writer registered for type: " + clazz);

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      serializer.serialize(object, outputStream);

      synchronized (qDataWriter) {
        qDataWriter.appendWithSize(outputStream.toByteArray());
        qDataWriter.flush();
      }
    } catch (EOFException | SocketException e) {
      log.debug("Server writer has been closed");
    } catch (IOException e) {
      log.debug("Server writer has been closed", e);
    }
  }

  /**
   * Finds the appropriate serializer by walking up the type hierarchy. Interfaces are not checked.
   *
   * @return the appropriate serializer, or {@code null} if none was found
   */
  private Serializer<?> findSerializerForClass(Class<?> clazz) {
    Class<?> classToCheck = clazz;

    while (!objectWriters.containsKey(classToCheck) && classToCheck != Object.class) {
      classToCheck = classToCheck.getSuperclass();
    }

    return objectWriters.get(classToCheck);
  }

  @Override
  public void close() throws IOException {
    qDataWriter.close();
  }
}
