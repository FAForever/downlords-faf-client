package com.faforever.client.remote.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalDateDeserializerTest {

  private LocalDateDeserializer instance;
  private JsonElement json;
  private Type typeOfT;
  private JsonDeserializationContext context;

  @BeforeEach
  public void setUp() throws Exception {
    instance = LocalDateDeserializer.INSTANCE;

    json = mock(JsonElement.class);
    typeOfT = mock(Type.class);
    context = mock(JsonDeserializationContext.class);
  }

  @Test
  public void testDeserialize() throws Exception {
    when(json.getAsString()).thenReturn("01.07.2015");

    LocalDate localDate = instance.deserialize(json, typeOfT, context);

    assertEquals(LocalDate.parse("2015-07-01"), localDate);
  }

  @Test
  public void testDeserializeThrowsNpe() {
    assertThrows(NullPointerException.class, () -> instance.deserialize(json, typeOfT, context));
  }
}
