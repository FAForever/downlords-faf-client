package com.faforever.client.preferences;

import com.google.common.eventbus.EventBus;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.bridj.Platform;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PreferencesServiceTest {

  @Mock
  private PreferencesService instance;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    instance = new PreferencesService(eventBus);
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
    if (Platform.isWindows()) {
      assertThat(instance.getFafDataDirectory(), is(Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever")));
    } else {
      assertThat(instance.getFafDataDirectory(), is(Paths.get(System.getProperty("user.home")).resolve(".faforever")));
    }
  }

  @Test
  public void testGetFafReposDirectory() throws Exception {
    assertThat(instance.getPatchReposDirectory(), is(instance.getFafDataDirectory().resolve("repos")));
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
}
