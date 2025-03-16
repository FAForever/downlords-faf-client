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
import java.util.List;

/**
 * Reads data from Forged Alliance (the forgedalliance, not the lobby).
 */
public class GPGNetReader {

  private static final int MAX_ARGUMENTS = 10;

  private final LittleEndianDataInputStream inputStream;
  private final Charset charset = StandardCharsets.UTF_8;

  public GPGNetReader(InputStream inputStream) {
    this.inputStream = new LittleEndianDataInputStream(new BufferedInputStream(inputStream));
  }

  private Object[] readArguments() throws IOException {
    int numberOfChunks = readInt();

    if (numberOfChunks > MAX_ARGUMENTS) {
      throw new IOException("Too many arguments: " + numberOfChunks);
    }

    Object[] arguments = new Object[numberOfChunks];

    for (int argumentNumber = 0; argumentNumber < numberOfChunks; argumentNumber++) {
      GPGFieldType fieldType = GPGFieldType.fromId(inputStream.read());

      Object argument = switch (fieldType) {
        case Known.INT -> readInt();
        case Known.STRING -> readString().replace("/t", "\t").replace("/n", "\n");
        case Unknown unknown -> readString().replace("/t", "\t").replace("/n", "\n");
      };

      arguments[argumentNumber] = argument;
    }

    return arguments;
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
    Object[] arguments = readArguments();
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
