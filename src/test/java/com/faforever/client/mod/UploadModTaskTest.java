package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Paths;

import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UploadModTaskTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private UploadModTask instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafApiAccessor fafApiAccessor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new UploadModTask();
    instance.preferencesService = preferencesService;
    instance.fafApiAccessor = fafApiAccessor;

    when(preferencesService.getCacheDirectory()).thenReturn(tempFolder.getRoot().toPath().resolve("cache"));
  }

  @Test(expected = NullPointerException.class)
  public void testModPathNull() throws Exception {
    instance.setProgressListener(aFloat -> {
    });
    instance.call();
  }

  @Test(expected = NullPointerException.class)
  public void testProgressListenerNull() throws Exception {
    instance.setModPath(Paths.get("."));
    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    instance.setModPath(tempFolder.newFolder("test-mod").toPath());
    instance.setProgressListener(aFloat -> {
    });

    instance.call();

    // FIXME filename
    verify(fafApiAccessor).uploadMod(any(), "");

    assertThat(preferencesService.getCacheDirectory().toFile().list(), emptyArray());
  }
}
