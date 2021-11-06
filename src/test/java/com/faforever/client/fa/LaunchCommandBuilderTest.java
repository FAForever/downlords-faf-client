package com.faforever.client.fa;

import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.Faction;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LaunchCommandBuilderTest extends ServiceTest {

  private static LaunchCommandBuilder defaultBuilder() {
    return LaunchCommandBuilder.create()
        .executable(Path.of("test.exe"))
        .executableDecorator("%s")
        .logFile(Path.of("preferences.log"))
        .username("junit");
  }

  @Test
  public void testAllSet() throws Exception {
    assertNotNull(defaultBuilder().build());
  }

  @Test
  public void testExecutableNullThrowsException() throws Exception {
    assertThrows(IllegalStateException.class, () -> defaultBuilder().executable(null).build());
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

  @Test
  public void testUsernameNullNotAllowedIfUidSet() throws Exception {
    assertThrows(IllegalStateException.class, () -> defaultBuilder().uid(123).username(null).build());
  }

  @Test
  public void testUsernameNullAllowedIfUidNotSet() throws Exception {
    defaultBuilder().uid(null).username(null).build();
  }

  @Test
  public void testFactionNullAllowed() throws Exception {
    defaultBuilder().faction(null).build();
  }

  @Test
  public void testLogFileNullAllowed() throws Exception {
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

  @Test
  public void testExpectedPlayersNullThrowsNoException() throws Exception {
    defaultBuilder().expectedPlayers(null).build();
  }

  @Test
  public void testNumberOfGamesNullThrowsNoException() throws Exception {
    defaultBuilder().numberOfGames(null).build();
  }

  @Test
  public void testTeamNullThrowsNoException() throws Exception {
    defaultBuilder().team(null).build();
  }

  @Test
  public void testMapPositionNullThrowsNoException() throws Exception {
    defaultBuilder().mapPosition(null).build();
  }

  @Test
  public void testMapNullThrowsNoException() throws Exception {
    defaultBuilder().map(null).build();
  }

  @Test
  public void testGameOptionsNullThrowsNoException() throws Exception {
    defaultBuilder().gameOptions(null).build();
  }

  @Test
  public void testFactionAsString() throws Exception {
    List<String> build = defaultBuilder().faction(Faction.SERAPHIM).build();
    assertThat(build.get(4), is("/seraphim"));
  }

  @Test
  public void testCommandFormatWithSpaces() throws Exception {
    String pathWithSpaces = "mypath/with space/test.exe";
    assertThat(
        defaultBuilder()
            .executable(Path.of(pathWithSpaces))
            .executableDecorator("/path/to/my/wineprefix primusrun wine %s")
            .build(),
        contains(
            "/path/to/my/wineprefix", "primusrun", "wine", Path.of(pathWithSpaces).toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString()
        ));
  }

  @Test
  public void testCommandFormatWithRedundantQuotionMarks() throws Exception {
    assertThat(
        defaultBuilder()
            .executableDecorator("/path/to/my/wineprefix primusrun wine \"\"%s\"\"")
            .build(),
        contains(
            "/path/to/my/wineprefix", "primusrun", "wine", Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString()
        ));
  }

  @Test
  public void testRehost() throws Exception {
    assertThat(
        defaultBuilder().rehost(true).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/rehost"
        ));
  }

  @Test
  public void testGpgPort() throws Exception {
    assertThat(
        defaultBuilder().localGpgPort(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/gpgnet", "127.0.0.1:0"
        ));
  }

  @Test
  public void testReplayPort() throws Exception {
    assertThat(
        defaultBuilder().uid(0).localReplayPort(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/savereplay",
            "gpgnet://127.0.0.1:0/0/junit.SCFAreplay"
        ));
  }

  @Test
  public void testCountryPort() throws Exception {
    assertThat(
        defaultBuilder().country("USA").build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/country", "USA"
        ));
  }

  @Test
  public void testClan() throws Exception {
    assertThat(
        defaultBuilder().clan("USA").build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/clan", "USA"
        ));
  }

  @Test
  public void testReplayId() throws Exception {
    assertThat(
        defaultBuilder().replayId(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/replayid", "0"
        ));
  }

  @Test
  public void testNumberOfGames() throws Exception {
    assertThat(
        defaultBuilder().numberOfGames(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/numgames", "0"
        ));
  }

  @Test
  public void testTeam() throws Exception {
    assertThat(
        defaultBuilder().team(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/team", "0"
        ));
  }

  @Test
  public void testExpectedPlayers() throws Exception {
    assertThat(
        defaultBuilder().expectedPlayers(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/players", "0"
        ));
  }

  @Test
  public void testMapPosition() throws Exception {
    assertThat(
        defaultBuilder().mapPosition(0).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/startspot", "0"
        ));
  }

  @Test
  public void testMap() throws Exception {
    assertThat(
        defaultBuilder().map("hello").build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/map", "hello"
        ));
  }

  @Test
  public void testGameOptions() throws Exception {
    assertThat(
        defaultBuilder().gameOptions(Map.of("test", "option")).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/gameoptions", "test:option"
        ));
  }

  @Test
  public void testRating() throws Exception {
    assertThat(
        defaultBuilder().mean(0f).deviation(0f).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/mean", "0.0",
            "/deviation", "0.0"
        ));
  }

  @Test
  public void testReplay() throws Exception {
    assertThat(
        defaultBuilder().replayFile(Path.of("here")).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/replay", Path.of("here").toAbsolutePath().toString()
        ));

    URI uri = URI.create("test");
    
    assertThat(
        defaultBuilder().replayUri(uri).build(),
        contains(
            Path.of("test.exe").toAbsolutePath().toString(),
            "/init", "init.lua",
            "/nobugreport",
            "/log", Path.of("preferences.log").toAbsolutePath().toString(),
            "/replay", uri.toASCIIString()
        ));
  }
  
  

  @Test
  public void testUseDefaultExecutableDecoratorOnEmptyString() throws Exception {
    assertThat(
        defaultBuilder().executableDecorator("").build(),
        equalTo(defaultBuilder().build())
    );
    assertThat(
        defaultBuilder().executableDecorator(null).build(),
        equalTo(defaultBuilder().build())
    );
  }
}
