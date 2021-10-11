package com.faforever.client.fa;

import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.Faction;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

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
