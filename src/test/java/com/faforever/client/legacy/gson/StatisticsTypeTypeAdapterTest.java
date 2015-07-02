package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.StatisticsType;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatisticsTypeTypeAdapterTest {

  private StatisticsTypeTypeAdapter instance;

  @Before
  public void setUp() throws Exception {
    instance = new StatisticsTypeTypeAdapter();
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, StatisticsType.GLOBAL_90_DAYS);

    verify(out).value(StatisticsType.GLOBAL_90_DAYS.getString());
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
    when(in.nextString()).thenReturn(StatisticsType.GLOBAL_90_DAYS.getString());

    StatisticsType statisticsType = instance.read(in);

    assertEquals(StatisticsType.GLOBAL_90_DAYS, statisticsType);
  }

  @Test
  public void testReadNullReturnsUnknown() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    StatisticsType statisticsType = instance.read(in);

    assertEquals(StatisticsType.UNKNOWN, statisticsType);
  }

  @Test
  public void testReadGibberishReturnsUnknown() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    StatisticsType statisticsType = instance.read(in);

    assertEquals(StatisticsType.UNKNOWN, statisticsType);
  }
}
