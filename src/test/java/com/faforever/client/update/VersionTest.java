package com.faforever.client.update;

import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TODO this is not a service, it should not extend ServiceTest
public class VersionTest extends ServiceTest {

  @Test
  public void shouldFailOnNullFromVersion() {
    assertThrows(NullPointerException.class, () -> Version.shouldUpdate(null));
  }

  @Test
  public void shouldFailOnNullToVersion() {
    Version.shouldUpdate(null);
  }

  @Test
  public void shouldUpdate_same() {
    Version.currentVersion = new ComparableVersion("1.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(false));
  }

  @Test
  public void shouldUpdate_isNewerPatch() {
    Version.currentVersion = new ComparableVersion("1.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.1")), is(true));
  }

  @Test
  public void shouldUpdate_isNewerPatch_9_10() {
    Version.currentVersion = new ComparableVersion("1.0.9");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.10")), is(true));
  }

  @Test
  public void shouldUpdate_isNewerMinor() {
    Version.currentVersion = new ComparableVersion("1.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.1.0")), is(true));
  }

  @Test
  public void shouldUpdate_isNewerMajor() {
    Version.currentVersion = new ComparableVersion("1.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("2.0.0")), is(true));
  }

  @Test
  public void shouldUpdate_alpha() {
    Version.currentVersion = new ComparableVersion("1.0.0-alpha");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(true));
  }

  @Test
  public void shouldUpdate_beta() {
    Version.currentVersion = new ComparableVersion("1.0.0-beta");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(true));
  }

  @Test
  public void shouldUpdate_isOlder_alpha_beta() {
    Version.currentVersion = new ComparableVersion("1.0.0-alpha");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0-beta")), is(true));
  }

  @Test
  public void shouldUpdate_rc1() {
    Version.currentVersion = new ComparableVersion("1.0.0-RC1");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(true));
  }

  @Test
  public void shouldUpdate_isNewerRc9_rc10() {
    Version.currentVersion = new ComparableVersion("1.0.0-RC9");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0-RC10")), is(true));
  }

  @Test
  public void shouldUpdate_gibberish() {
    Version.currentVersion = new ComparableVersion("1.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("xyz")), is(false));
  }

  @Test
  public void shouldUpdate_isOlderPatch_10_9() {
    Version.currentVersion = new ComparableVersion("1.0.10");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.9")), is(false));
  }

  @Test
  public void shouldUpdate_isOlder_beta_alpha() {
    Version.currentVersion = new ComparableVersion("1.0.0-beta");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0-alpha")), is(false));
  }

  @Test
  public void shouldUpdate_isOlder_minor() {
    Version.currentVersion = new ComparableVersion("1.1.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(false));
  }

  @Test
  public void shouldUpdate_isOlder_major() {
    Version.currentVersion = new ComparableVersion("2.0.0");
    assertThat(Version.shouldUpdate(new ComparableVersion("1.0.0")), is(false));
  }

  @Test
  public void shouldUpdate_unspecifiedVersion() {
    Version.currentVersion = Version.UNSPECIFIED_VERSION;
    assertThat(Version.shouldUpdate(new ComparableVersion("9.9.99")), is(false));
  }
}