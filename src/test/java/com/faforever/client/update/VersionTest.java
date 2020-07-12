package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionTest {

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullFromVersion() {
    Version.isNewer(new ComparableVersion("1.0.0"));
  }

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullToVersion() {
    Version.isNewer(null);
  }

  @Test
  public void shouldUpdateIfRemoteIsNewer() {
    assertFalse(Version.isNewer(new ComparableVersion("1.1.0")));
    assertFalse(Version.isNewer(new ComparableVersion("1.1.0")));
    assertTrue(Version.isNewer(new ComparableVersion("1.0.0-beta")));
    assertTrue(Version.isNewer(new ComparableVersion("1.0.0-RC1")));
    assertTrue(Version.isNewer(new ComparableVersion("1.0.0-RC2")));
    assertFalse(Version.isNewer(new ComparableVersion("1.0.0-alpha")));
    assertTrue(Version.isNewer(new ComparableVersion("1.1.0")));
    assertFalse(Version.isNewer(new ComparableVersion("1.0.9")));
    assertTrue(Version.isNewer(new ComparableVersion("1.1.0")));
    assertFalse(Version.isNewer(new ComparableVersion("1.0.9")));
    assertTrue(Version.isNewer(new ComparableVersion("1.1.1")));
    assertTrue(Version.isNewer(new ComparableVersion("2.0.0")));
  }

  @Test
  public void shouldNotUpdateIfSnapshot() {
    assertThat(Version.isNewer(new ComparableVersion("9.9.99")), is(false));
  }

  @Test
  public void shouldNotUpdateIfToVersionIsNotSemver() {
    assertFalse(Version.isNewer(new ComparableVersion("xyz")));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsSame() {
    assertFalse(Version.isNewer(new ComparableVersion("1.1.5")));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsOlder() {
    assertFalse(Version.isNewer(new ComparableVersion("1.1.5")));
  }
}