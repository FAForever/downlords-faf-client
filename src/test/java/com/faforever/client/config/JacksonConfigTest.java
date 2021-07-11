package com.faforever.client.config;

import com.faforever.client.config.JacksonConfig.UrlDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class JacksonConfigTest {

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
