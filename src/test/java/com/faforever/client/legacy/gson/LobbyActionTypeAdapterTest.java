package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.relay.LobbyAction;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LobbyActionTypeAdapterTest {

  private RelayServerActionTypeAdapter instance;

  @Before
  public void setUp() throws Exception {
    instance = RelayServerActionTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, LobbyAction.CONNECTED);

    verify(out).value(LobbyAction.CONNECTED.getString());
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
    when(in.nextString()).thenReturn(LobbyAction.CHAT.getString());

    LobbyAction lobbyAction = instance.read(in);

    assertEquals(LobbyAction.CHAT, lobbyAction);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadNullThrowsIae() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    instance.read(in);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReadGibberishThrowsIae() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    instance.read(in);
  }
}
