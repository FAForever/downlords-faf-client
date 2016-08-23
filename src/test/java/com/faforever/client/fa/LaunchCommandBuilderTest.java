package com.faforever.client.fa;

import com.faforever.client.game.GameType;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class LaunchCommandBuilderTest {

  private static LaunchCommandBuilder defaultBuilder() {
    return LaunchCommandBuilder.create()
        .executable(Paths.get("test.exe"))
        .logFile(Paths.get("game.log"))
        .gameType(GameType.DEFAULT.getString())
        .username("junit");
  }

  @Test
  public void testAllSet() throws Exception {
    assertNotNull(defaultBuilder().build());
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
  public void testUsernameNullNotAllowedIfUidSet() throws Exception {
    defaultBuilder().uid(123).username(null).build();
  }

  @Test
  public void testUsernameNullAllowedIfUidNotSet() throws Exception {
    defaultBuilder().uid(null).username(null).build();
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

  @Test(expected = IllegalStateException.class)
  public void testCommandFormatNullNotAllowed() throws Exception {
    defaultBuilder().executableDecorator(null).build();
  }

  @Test
  public void testCommandFormat() throws Exception {
    assertThat(
        defaultBuilder()
            .executableDecorator("/path/to/my/wineprefix primusrun wine %s")
            .build(),
        contains(
            "/path/to/my/wineprefix", "primusrun", "wine", Paths.get("test.exe").toAbsolutePath().toString(),
            "/init", "init_faf.lua",
            "/nobugreport",
            "/log", Paths.get("game.log").toAbsolutePath().toString()
        ));
  }
}
