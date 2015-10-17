package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.VictoryCondition;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VictoryConditionTypeAdapterTest {

  private VictoryConditionTypeAdapter instance;

  @Before
  public void setUp() throws Exception {
    instance = VictoryConditionTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, VictoryCondition.DEMORALIZATION);

    verify(out).value((int) VictoryCondition.DEMORALIZATION.getValue());
  }

  @Test
  public void testWriteNull() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, null);

    verify(out).value((String) VictoryCondition.UNKNOWN.getValue());
  }

  @Test
  public void testReadNormal() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(String.valueOf(VictoryCondition.DOMINATION.getValue()));

    VictoryCondition victoryCondition = instance.read(in);

    assertEquals(VictoryCondition.DOMINATION, victoryCondition);
  }

  @Test
  public void testReadNullValue() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    VictoryCondition victoryCondition = instance.read(in);

    assertEquals(VictoryCondition.UNKNOWN, victoryCondition);
  }
}
