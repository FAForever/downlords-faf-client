package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GpgGameMessageTypeAdapterTest {

  private GpgClientMessageTypeAdapter instance;

  @Before
  public void setUp() throws Exception {
    instance = GpgClientMessageTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, GpgClientCommand.CONNECTED);

    verify(out).value(GpgClientCommand.CONNECTED.getString());
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
    when(in.nextString()).thenReturn(GpgClientCommand.CHAT.getString());

    GpgClientCommand gpgClientCommand = instance.read(in);

    assertEquals(GpgClientCommand.CHAT, gpgClientCommand);
  }

  public void testReadNullReturnsNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    assertNull(instance.read(in));
  }

  public void testReadGibberishReturnsNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    assertNull(instance.read(in));
  }
}
