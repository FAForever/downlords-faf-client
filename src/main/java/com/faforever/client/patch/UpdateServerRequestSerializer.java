package com.faforever.client.patch;

import com.faforever.client.legacy.io.QDataWriter;
import com.faforever.client.patch.domain.UpdateServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum UpdateServerRequestSerializer implements Serializer<UpdateServerRequest> {

  INSTANCE;

  private static final String CONFIDENTIAL_INFORMATION_MASK = "********";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void serialize(UpdateServerRequest object, OutputStream outputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    QDataWriter qDataWriter = new QDataWriter(byteArrayOutputStream);
    qDataWriter.append(object.getUpdateServerCommand().name());

    for (Object arg : object.getArgs()) {
      if (arg instanceof Double) {
        qDataWriter.writeInt32(((Double) arg).intValue());
      } else if (arg instanceof Integer) {
        qDataWriter.writeInt32((int) arg);
      } else if (arg instanceof String) {
        qDataWriter.append((String) arg);
      }
    }

    qDataWriter.flush();

    byte[] byteArray = byteArrayOutputStream.toByteArray();

    if (logger.isDebugEnabled()) {
      // Remove the first 4 bytes which contain the length of the following data
      String data = new String(Arrays.copyOfRange(byteArray, 4, byteArray.length), StandardCharsets.UTF_16BE);

      for (String stringToMask : object.getStringsToMask()) {
        data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"");
      }

      logger.debug("Writing to server: {}", data);
    }

    outputStream.write(byteArray);
  }
}
