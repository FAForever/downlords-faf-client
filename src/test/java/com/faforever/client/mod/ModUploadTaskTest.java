package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModUploadTaskTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private ModUploadTask instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    instance = new ModUploadTask();
    instance.preferencesService = preferencesService;
    instance.fafApiAccessor = fafApiAccessor;
    instance.i18n = i18n;

    when(preferencesService.getCacheDirectory()).thenReturn(tempFolder.getRoot().toPath().resolve("cache"));
    when(i18n.get(any())).thenReturn("");
  }

  @Test(expected = NullPointerException.class)
  public void testModPathNull() throws Exception {
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

    instance.call();

    verify(fafApiAccessor).uploadMod(any(), anyBoolean(), any());

    assertThat(Files.list(preferencesService.getCacheDirectory()).toArray(), emptyArray());
  }
}
