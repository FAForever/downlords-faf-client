package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.GameState;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameStateTypeAdapterTest {

  private GameStateTypeAdapter instance;

  @Before
  public void setUp() throws Exception {
    instance = new GameStateTypeAdapter();
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, GameState.OPEN);

    verify(out).value(GameState.OPEN.getString());
  }

  @Test
  public void testWriteNullWritesUnknown() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, null);

    verify(out).value(GameState.UNKNOWN.getString());
  }

  @Test
  public void testRead() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(GameState.OPEN.getString());

    GameState gameState = instance.read(in);

    assertEquals(GameState.OPEN, gameState);
  }

  @Test
  public void testReadNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    GameState gameState = instance.read(in);

    assertEquals(GameState.UNKNOWN, gameState);
  }

  @Test
  public void testReadGibberish() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    GameState gameState = instance.read(in);

    assertEquals(GameState.UNKNOWN, gameState);
  }
}
