package com.faforever.client.remote.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalTimeDeserializerTest {

  private LocalTimeDeserializer instance;
  private JsonElement json;
  private Type typeOfT;
  private JsonDeserializationContext context;

  @Before
  public void setUp() throws Exception {
    instance = LocalTimeDeserializer.INSTANCE;

    json = mock(JsonElement.class);
    typeOfT = mock(Type.class);
    context = mock(JsonDeserializationContext.class);
  }

  @Test
  public void testDeserialize() throws Exception {
    when(json.getAsString()).thenReturn("16:07");

    LocalTime localTime = instance.deserialize(json, typeOfT, context);

    assertEquals(LocalTime.parse("16:07"), localTime);
  }

  @Test(expected = NullPointerException.class)
  public void testDeserializeThrowsNpe() {
    instance.deserialize(json, typeOfT, context);
  }
}
