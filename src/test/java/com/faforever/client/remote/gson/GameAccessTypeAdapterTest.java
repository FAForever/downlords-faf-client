package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.test.ServiceTest;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameAccessTypeAdapterTest extends ServiceTest {

  private GameAccessTypeAdapter instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = GameAccessTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, GameAccess.PUBLIC);

    verify(out).value(GameAccess.PUBLIC.getString());
  }

  @Test
  public void testWriteNull() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, null);

    verify(out).nullValue();
  }

  @Test
  public void testRead() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(GameAccess.PASSWORD.getString());

    GameAccess gameAccess = instance.read(in);

    assertEquals(GameAccess.PASSWORD, gameAccess);
  }

  @Test
  public void testReadNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    GameAccess gameAccess = instance.read(in);

    assertNull(gameAccess);
  }

  @Test
  public void testReadGibberish() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    GameAccess gameAccess = instance.read(in);

    assertNull(gameAccess);
  }
}
