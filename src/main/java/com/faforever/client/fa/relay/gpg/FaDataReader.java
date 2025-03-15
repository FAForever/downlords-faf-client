package com.faforever.client.fa.relay.gpg;

import com.faforever.client.fa.relay.gpg.GPGFieldType.Known;
import com.faforever.client.fa.relay.gpg.GPGFieldType.Unknown;
import com.faforever.client.fa.relay.gpg.GPGMessage.CreateLobby;
import com.faforever.client.fa.relay.gpg.GPGMessage.GameEnded;
import com.faforever.client.fa.relay.gpg.GPGMessage.Generic;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads data from Forged Alliance (the forgedalliance, not the lobby).
 */
public class FaDataReader {

  private static final int MAX_CHUNK_SIZE = 10;
  private static final int FIELD_TYPE_INT = 0;

  private final LittleEndianDataInputStream inputStream;
  private final Charset charset = StandardCharsets.UTF_8;

  public FaDataReader(InputStream inputStream) {
    this.inputStream = new LittleEndianDataInputStream(new BufferedInputStream(inputStream));
  }

  private Object[] readChunks() throws IOException {
    int numberOfChunks = readInt();

    if (numberOfChunks > MAX_CHUNK_SIZE) {
      throw new IOException("Too many chunks: " + numberOfChunks);
    }

    Object[] chunks = new Object[numberOfChunks];

    for (int chunkNumber = 0; chunkNumber < numberOfChunks; chunkNumber++) {
      GPGFieldType fieldType = GPGFieldType.fromId(inputStream.read());

      Object chunk = switch (fieldType) {
        case Known.INT -> readInt();
        case Known.STRING -> readString().replace("/t", "\t").replace("/n", "\n");
        case Unknown unknown -> readString().replace("/t", "\t").replace("/n", "\n");
      };

      chunks[chunkNumber] = chunk;
    }

    return chunks;
  }

  private int readInt() throws IOException {
    return inputStream.readInt();
  }

  private String readString() throws IOException {
    int size = readInt();

    byte[] buffer = new byte[size];
    inputStream.readFully(buffer);
    return new String(buffer, charset);
  }

  public GPGMessage readMessage() throws IOException {
    String command = readString();
    Object[] arguments = readChunks();
    return createMessage(command, arguments);
  }

  private GPGMessage createMessage(String command, Object... arguments) {
    return switch (command) {
      case "GameState" ->
          new GPGMessage.GameState(com.faforever.client.fa.relay.gpg.GameState.fromName((String) arguments[0]));
      case "GameEnded" -> new GameEnded();
      case "CreateLobby" ->
          new CreateLobby(LobbyInitMode.fromId((int) arguments[0]), (int) arguments[1], (String) arguments[2],
                          (int) arguments[3]);
      default -> new Generic(command, List.of(arguments));
    };
  }
}
