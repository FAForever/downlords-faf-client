package com.faforever.client.discord;

import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

public class DiscordLaunchRegistrationTest extends ServiceTest {

  private final List<List<String>> values = new ArrayList<>();

  @Test
  public void registersLauncher() {
    Path launcher = Path.of("C:", "Program Files", "FAF Client", "faf-client.exe");

    DiscordLaunchRegistration.register("1234", Optional.of(launcher), this::record);

    String key = "Software\\Classes\\discord-1234";
    assertThat(values, contains(
        List.of(key, "", "URL:Run game 1234 protocol"),
        List.of(key, "URL Protocol", ""),
        List.of(key + "\\DefaultIcon", "", launcher.toString()),
        List.of(key + "\\shell\\open\\command", "", "\"" + launcher + "\"")
    ));
  }

  @Test
  public void skipsJava() {
    DiscordLaunchRegistration.register("1234", Optional.of(Path.of("jdk", "bin", "javaw.exe")), this::record);
    DiscordLaunchRegistration.register("1234", Optional.of(Path.of("jdk", "bin", "JAVA.EXE")), this::record);

    assertThat(values, empty());
  }

  @Test
  public void skipsUnknownExecutable() {
    DiscordLaunchRegistration.register("1234", Optional.empty(), this::record);

    assertThat(values, empty());
  }

  private void record(String key, String name, String value) {
    values.add(List.of(key, name, value));
  }
}
