package com.faforever.client.legacy.writer;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.io.QStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Serializer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends data to the server. Classes should not use the server writer directly, but {@link LobbyServerAccessor} instead.
 * TODO improve javadoc, explain that there are different ways to write to the server
 */
public class ServerWriter implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final QStreamWriter qStreamWriter;
  private Map<Class<?>, Serializer<?>> objectWriters;

  public ServerWriter(OutputStream outputStream) throws IOException {
    qStreamWriter = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(outputStream)));
    objectWriters = new HashMap<>();
  }

  public void registerObjectWriter(Serializer<?> objectSerializer, Class<?> writableClass) {
    objectWriters.put(writableClass, objectSerializer);
  }

  @SuppressWarnings("unchecked")
  public void write(Object object){
    Class<?> clazz = object.getClass();

    Serializer<Object> serializer = (Serializer<Object>) findSerializerForClass(clazz);

    if (serializer == null) {
      throw new IllegalStateException("No object writer registered for type: " + clazz);
    }

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      serializer.serialize(object, outputStream);

      synchronized (qStreamWriter) {
        qStreamWriter.appendWithSize(outputStream.toByteArray());
        qStreamWriter.flush();
      }
    } catch (EOFException | SocketException e) {
      logger.debug("Server writer has been closed");
    } catch (IOException e) {
      logger.debug("Server writer has been closed", e);
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
    qStreamWriter.close();
  }
}
