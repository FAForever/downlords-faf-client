package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.GameStatus;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameStatusTypeAdapterTest {

  private GameStateTypeAdapter instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = GameStateTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, GameStatus.OPEN);

    verify(out).value(GameStatus.OPEN.getString());
  }

  @Test
  public void testWriteNullWritesUnknown() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, null);

    verify(out).value(GameStatus.UNKNOWN.getString());
  }

  @Test
  public void testRead() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(GameStatus.OPEN.getString());

    GameStatus gameStatus = instance.read(in);

    assertEquals(GameStatus.OPEN, gameStatus);
  }

  @Test
  public void testReadNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    GameStatus gameStatus = instance.read(in);

    assertEquals(GameStatus.UNKNOWN, gameStatus);
  }

  @Test
  public void testReadGibberish() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    GameStatus gameStatus = instance.read(in);

    assertEquals(GameStatus.UNKNOWN, gameStatus);
  }
}
