package com.faforever.client.net;

import org.mockito.ArgumentMatcher;

import java.net.URI;

public class UriStartingWithMatcher implements ArgumentMatcher<URI> {

  private String prefix;

  public UriStartingWithMatcher(String prefix) {
    this.prefix = prefix;
  }

  public static URI uriStartingWith(String prefix) {
    return Matchers.argThat(new UriStartingWithMatcher(prefix));
  }

  @Override
  public boolean matches(URI argument) {
    return ((URI) argument).toASCIIString().startsWith(prefix);
  }
}
