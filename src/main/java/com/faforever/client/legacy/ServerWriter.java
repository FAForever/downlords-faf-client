package com.faforever.client.legacy;

import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameTypeTypeAdapter;
import com.faforever.client.legacy.gson.RelayServerActionTypeAdapter;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameType;
import com.faforever.client.legacy.domain.ServerWritable;
import com.faforever.client.legacy.relay.RelayServerAction;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Sends data to the server. Classes should not use the server writer directly, but {@link ServerAccessor} instead.
 */
public class ServerWriter implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String CONFIDENTIAL_INFORMATION_MASK = "********";

  private final QStreamWriter writer;
  private final Gson gson;
  private boolean appendUsername;
  private boolean appendSession;
  private CharSequence username;
  private CharSequence session;
  private boolean closedGracefully;

  public ServerWriter(OutputStream outputStream) throws IOException {
    writer = new QStreamWriter(new DataOutputStream(new BufferedOutputStream(outputStream)));
    gson = new GsonBuilder()
        .disableHtmlEscaping()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(GameType.class, new GameTypeTypeAdapter())
        .registerTypeAdapter(GameState.class, new GameStateTypeAdapter())
        .registerTypeAdapter(RelayServerAction.class, new RelayServerActionTypeAdapter())
        .create();
  }

  public void setAppendUsername(boolean appendUsername) {
    this.appendUsername = appendUsername;
  }

  public void setAppendSession(boolean appendSession) {
    this.appendSession = appendSession;
  }

  public void setUsername(CharSequence username) {
    this.username = username;
  }

  public void setSession(CharSequence session) {
    this.session = session;
  }

  public void write(ServerWritable serverWritable) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      Writer stringWriter = new StringWriter();
      serverWritable.write(gson, stringWriter);

      QStreamWriter qStreamWriter = new QStreamWriter(byteArrayOutputStream);
      qStreamWriter.appendString(stringWriter.toString());

      if (appendUsername) {
        qStreamWriter.appendString(username);
      }
      if (appendSession) {
        qStreamWriter.appendString(session);
      }

      byte[] byteArray = byteArrayOutputStream.toByteArray();

      if (logger.isDebugEnabled()) {
        // Remove the first 4 bytes which contain the length of the following data
        String data = new String(Arrays.copyOfRange(byteArray, 4, byteArray.length), StandardCharsets.UTF_16BE);

        for (String stringToMask : serverWritable.getStringsToMask()) {
          data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"");
        }

        logger.debug("Writing to server: {}", data);
      }

      synchronized (writer) {
        writer.append(byteArray);
        writer.flush();
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
    writer.close();
  }
}
