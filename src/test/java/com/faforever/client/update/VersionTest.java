package com.faforever.client.update;

import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionTest extends ServiceTest {

  @Test
  public void shouldFailOnNullFromVersion() {
    assertThrows(NullPointerException.class, () -> Version.shouldUpdate(null, "v1.0.0"));
  }

  @Test
  public void shouldFailOnNullToVersion() {
    assertThrows(NullPointerException.class, () -> Version.shouldUpdate("v1.0.0", null));
  }

  @Test
  public void shouldUpdateIfRemoteIsNewer() {
    assertFalse(Version.shouldUpdate("1.1.0", "1.1.0"));
    assertFalse(Version.shouldUpdate("1.1.0", "v1.1.0"));
    assertTrue(Version.shouldUpdate("1.0.0-alpha", "v1.0.0-beta"));
    assertTrue(Version.shouldUpdate("1.0.0-alpha", "v1.0.0-RC1"));
    assertTrue(Version.shouldUpdate("1.0.0-RC1", "v1.0.0-RC2"));
    assertFalse(Version.shouldUpdate("1.0.0-beta", "v1.0.0-alpha"));
    assertTrue(Version.shouldUpdate("1.0.9", "v1.1.0"));
    assertFalse(Version.shouldUpdate("1.1.0", "v1.0.9"));
    assertTrue(Version.shouldUpdate("v1.0.9", "1.1.0"));
    assertFalse(Version.shouldUpdate("1.1.0", "1.0.9"));
    assertTrue(Version.shouldUpdate("1.1.0", "v1.1.1"));
    assertTrue(Version.shouldUpdate("1.9.9", "v2.0.0"));
  }

  @Test
  public void shouldNotUpdateIfSnapshot() {
    assertFalse(Version.shouldUpdate("snapshot", "v9.9.99"));
  }

  @Test
  public void shouldNotUpdateIfToVersionIsNotSemver() {
    assertFalse(Version.shouldUpdate("1.1.7", "xyz"));
  }

  @Test
  public void shouldFailIfToVersionIsNotSemver() {
    assertThrows(IllegalArgumentException.class, () -> assertFalse(Version.shouldUpdate("xyz", "1.1.5")));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsSame() {
    assertFalse(Version.shouldUpdate("1.1.5", "v1.1.5"));
  }

  @Test
  public void shouldNotUpdateIfRemoteIsOlder() {
    assertFalse(Version.shouldUpdate("1.1.9", "v1.1.5"));
  }

  public static void setCurrentVersion(String version) throws NoSuchFieldException, IllegalAccessException {
    Field field = Version.class.getDeclaredField("currentVersion");
    field.setAccessible(true);
    field.set(null, version);
  }
}