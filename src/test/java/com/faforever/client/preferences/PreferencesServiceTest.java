package com.faforever.client.preferences;

import com.faforever.client.os.OperatingSystem;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PreferencesServiceTest {

  private PreferencesService instance;

  @Before
  public void setUp() throws Exception {
    instance = new PreferencesService();
  }

  @Test
  public void testGetPreferencesDirectory() throws Exception {
    assertThat(instance.getPreferencesDirectory(), notNullValue());
  }

  @Test
  public void testGetFafBinDirectory() throws Exception {
    assertThat(instance.getFafBinDirectory(), is(instance.getFafDataDirectory().resolve("bin")));
  }

  @Test
  public void testGetFafDataDirectory() throws Exception {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        assertThat(instance.getFafDataDirectory(), is(Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever")));
        break;

      default:
        assertThat(instance.getFafDataDirectory(), is(Paths.get(System.getProperty("user.home")).resolve(".faforever")));
    }
  }

  @Test
  public void testGetFafReposDirectory() throws Exception {
    assertThat(instance.getGitReposDirectory(), is(instance.getFafDataDirectory().resolve("repos")));
  }

  @Test
  public void testAddUpdateListener() throws Exception {

  }

  @Test
  public void testGetCorruptedReplaysDirectory() throws Exception {
    Path result = instance.getCorruptedReplaysDirectory();
    Path expected = instance.getReplaysDirectory().resolve("corrupt");
    assertThat(result, is(expected));
  }

  @Test
  public void testGetReplaysDirectory() throws Exception {
    assertThat(instance.getReplaysDirectory(), is(instance.getFafDataDirectory().resolve("replays")));
  }

  @Test
  public void testGetCacheDirectory() throws Exception {
    assertThat(instance.getCacheDirectory(), is(instance.getFafDataDirectory().resolve("cache")));
  }

  @Test
  public void testGetFafLogDirectory() throws Exception {
    assertThat(instance.getFafLogDirectory(), is(instance.getFafDataDirectory().resolve("logs")));
  }

  @Test
  public void testConfigureLogging() throws Exception {
    PreferencesService.configureLogging();
  }
}
