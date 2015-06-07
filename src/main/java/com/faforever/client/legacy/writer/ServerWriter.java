package com.faforever.client.legacy.writer;

import com.faforever.client.legacy.ServerAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Serializer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends data to the server. Classes should not use the server writer directly, but {@link ServerAccessor} instead.
 * TODO improve javadoc, explain that there are different ways to write to the server
 */
public class ServerWriter implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final QStreamWriter qStreamWriter;
  private boolean closedGracefully;
  private Map<Class<?>, Serializer<?>> objectWriters;

  public ServerWriter(OutputStream outputStream) throws IOException {
    qStreamWriter = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(outputStream)));
    objectWriters = new HashMap<>();
  }

  public void registerObjectWriter(Serializer<?> objectSerializer, Class<?> writableClass) {
    objectWriters.put(writableClass, objectSerializer);
  }

  public void write(Object object){
    Class<?> type = object.getClass();
    if (!objectWriters.containsKey(type)) {
      throw new IllegalStateException("No object writer registered for type: " + type);
    }

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      ((Serializer<Object>) objectWriters.get(type)).serialize(object, outputStream);

      synchronized (qStreamWriter) {
        qStreamWriter.appendWithSize(outputStream.toByteArray());
        qStreamWriter.flush();
      }
    } catch (IOException e) {
      if (!closedGracefully) {
        throw new RuntimeException(e);
      } else {
        logger.debug("Server writer has been closed");
      }
    }
  }

  @Override
  public void close() throws IOException {
    closedGracefully = true;
    qStreamWriter.close();
  }


}
