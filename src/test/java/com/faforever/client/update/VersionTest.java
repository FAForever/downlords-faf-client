package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionTest {

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullFromVersion() {
    Version.shouldUpdate(null, new ComparableVersion("1.0.0"));
  }

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullToVersion() {
    Version.shouldUpdate(new ComparableVersion("1.0.0"), null);
  }

  @Test
  public void shouldUpdateIfRemoteIsNewer() {
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.0"), new ComparableVersion("1.1.0")));
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.0"), new ComparableVersion("1.1.0")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.0.0-alpha"), new ComparableVersion("1.0.0-beta")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.0.0-alpha"), new ComparableVersion("1.0.0-RC1")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.0.0-RC1"), new ComparableVersion("1.0.0-RC2")));
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.0.0-beta"), new ComparableVersion("1.0.0-alpha")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.0.9"), new ComparableVersion("1.1.0")));
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.0"), new ComparableVersion("1.0.9")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.0.9"), new ComparableVersion("1.1.0")));
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.0"), new ComparableVersion("1.0.9")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.1.0"), new ComparableVersion("1.1.1")));
    assertTrue(Version.shouldUpdate(new ComparableVersion("1.9.9"), new ComparableVersion("2.0.0")));
  }

  @Test
  public void shouldNotUpdateIfSnapshot() {
    assertThat(Version.shouldUpdate(Version.UNSPECIFIED_VERSION, new ComparableVersion("9.9.99")), is(false));
  }

  @Test
  public void shouldNotUpdateIfToVersionIsNotSemver() {
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.7"), new ComparableVersion("xyz")));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsSame() {
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.5"), new ComparableVersion("1.1.5")));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsOlder() {
    assertFalse(Version.shouldUpdate(new ComparableVersion("1.1.9"), new ComparableVersion("1.1.5")));
  }
}