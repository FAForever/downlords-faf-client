package com.faforever.client.fa;

import com.faforever.client.game.GameType;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class LaunchCommandBuilderTest {

  @Test
  public void testAllSet() throws Exception {
    assertNotNull(defaultBuilder().build());
  }

  private static LaunchCommandBuilder defaultBuilder() {
    return LaunchCommandBuilder.create()
        .executable(Paths.get("."))
        .logFile(Paths.get("."))
        .gameType(GameType.DEFAULT.getString())
        .username("junit");
  }

  @Test(expected = IllegalStateException.class)
  public void testExecutableNullThrowsException() throws Exception {
    defaultBuilder().executable(null).build();
  }

  @Test
  public void testUidNullAllowed() throws Exception {
    defaultBuilder().uid(null).build();
  }

  @Test
  public void testMeanNullAllowed() throws Exception {
    defaultBuilder().mean(null).build();
  }

  @Test
  public void testDeviationNullAllowed() throws Exception {
    defaultBuilder().deviation(null).build();
  }

  @Test
  public void testCountryNullAllowed() throws Exception {
    defaultBuilder().country(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testUsernameNullNotAllowed() throws Exception {
    defaultBuilder().username(null).build();
  }

  @Test
  public void testFactionNullAllowed() throws Exception {
    defaultBuilder().faction(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testGameTypeNullThrowsException() throws Exception {
    defaultBuilder().gameType(null).build();
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
