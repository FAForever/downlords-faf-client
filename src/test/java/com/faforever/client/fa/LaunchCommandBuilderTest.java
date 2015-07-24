package com.faforever.client.fa;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static junit.framework.Assert.assertNotNull;

public class LaunchCommandBuilderTest {

  private static LaunchCommandBuilder defaultBuilder() {
    return LaunchCommandBuilder.create()
        .additionalArgs(Collections.emptyList())
        .country("")
        .clan("")
        .deviation(0f)
        .mean(0f)
        .localGpgPort(0)
        .logFile(Paths.get("."))
        .username("")
        .uid(0)
        .executable(Paths.get("."));
  }

  @Test
  public void testAllSet() throws Exception {
    assertNotNull(defaultBuilder().build());
  }

  @Test(expected = IllegalStateException.class)
  public void testExecutableNullThrowsException() throws Exception {
    defaultBuilder().executable(null).build();
  }

  public void testUidNullAllowed() throws Exception {
    defaultBuilder().uid(null).build();
  }

  public void testMeanNullAllowed() throws Exception {
    defaultBuilder().mean(null).build();
  }

  public void testDeviationNullAllowed() throws Exception {
    defaultBuilder().deviation(null).build();
  }

  public void testCountryNullAllowed() throws Exception {
    defaultBuilder().country(null).build();
  }

  public void testUsernameNullAllowed() throws Exception {
    defaultBuilder().username(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testLogFileNullThrowsException() throws Exception {
    defaultBuilder().logFile(null).build();
  }

  @Test
  public void testAdditionalArgsNullThrowsNoException() throws Exception {
    defaultBuilder().additionalArgs(null).build();
  }

  @Test
  public void testClanNullThrowsNoException() throws Exception {
    defaultBuilder().clan(null).build();
  }
}
