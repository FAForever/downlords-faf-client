package com.faforever.client.remote;

import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.io.QDataWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Sends data to the server. Classes should not use the server writer directly, but e.g. {@link
 * com.faforever.client.remote.FafService} or any other server accessor instead.
 */
@Slf4j
public class ServerWriter implements Closeable {
  private static final String CONFIDENTIAL_INFORMATION_MASK = "********";

  private final ObjectMapper objectMapper;
  private final QDataWriter qDataWriter;

  public ServerWriter(OutputStream outputStream, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    qDataWriter = new QDataWriter(new DataOutputStream(new BufferedOutputStream(outputStream)));
  }

  @SuppressWarnings("unchecked")
  public void write(SerializableMessage message) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      Writer jsonStringWriter = new StringWriter();

      objectMapper.writeValue(jsonStringWriter, message);

      QDataWriter qDataWriter = new QDataWriter(outputStream);
      qDataWriter.append(jsonStringWriter.toString());
      byte[] byteArray = outputStream.toByteArray();

      if (log.isDebugEnabled()) {
        // Remove the first 4 bytes which contain the length of the following data
        String data = new String(Arrays.copyOfRange(byteArray, 4, byteArray.length), StandardCharsets.UTF_16BE);
        for (String stringToMask : message.getStringsToMask()) {
          data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"");
        }
        log.debug("Writing to server: {}", data);
      }

      outputStream.write(byteArray);
      this.qDataWriter.appendWithSize(byteArray);
      this.qDataWriter.flush();
    } catch (EOFException | SocketException e) {
      log.debug("Server writer has been closed");
    } catch (IOException e) {
      log.debug("Server writer has been closed", e);
    }
  }

  @Override
  public void close() throws IOException {
    qDataWriter.close();
  }
}
