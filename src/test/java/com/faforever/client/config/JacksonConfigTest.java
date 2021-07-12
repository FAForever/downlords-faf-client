package com.faforever.client.config;

import com.faforever.client.config.JacksonConfig.UrlDeserializer;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonConfigTest extends ServiceTest {

  @Mock
  JsonParser parser;

  @Test
  public void testUrlDeserializer() throws IOException {
    String urlOriginal = "http://www.foo.bar/path with space/test?foo=arg with space&bar=baz";
    String urlEncoded = "http://www.foo.bar/path%20with%20space/test?foo=arg%20with%20space&bar=baz";
    
    UrlDeserializer deserializer = new UrlDeserializer();
    Mockito.when(parser.getValueAsString()).thenReturn(urlOriginal);
    URL result = deserializer.deserialize(parser, null);
    assertEquals(urlEncoded, result.toString());
  }
}
