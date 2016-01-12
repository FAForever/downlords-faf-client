package com.faforever.client.net;

import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;

import java.net.URI;

public class UriStartingWithMatcher implements ArgumentMatcher<URI> {

  private String prefix;

  public UriStartingWithMatcher(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public boolean matches(Object argument) {
    return ((URI) argument).toASCIIString().startsWith(prefix);
  }

  public static URI uriStartingWith(String prefix) {
    return Matchers.argThat(new UriStartingWithMatcher(prefix));
  }
}
