package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModUploadTaskTest extends AbstractPlainJavaFxTest{

  @TempDir
  public Path tempFolder;
  private ModUploadTask instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafService fafService;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ModUploadTask(preferencesService, fafService, i18n);

    Path cacheDirectory = Files.createDirectories(tempFolder.resolve("cache"));
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory);
    when(i18n.get(any())).thenReturn("");
  }

  @Test
  public void testModPathNull() throws Exception {
    assertThrows(NullPointerException.class, () -> instance.call());
  }

  @Test
  public void testProgressListenerNull() throws Exception {
    instance.setModPath(Paths.get("."));
    assertThrows(NullPointerException.class, () -> instance.call());
  }

  @Test
  public void testCall() throws Exception {
    instance.setModPath(Files.createDirectories(tempFolder.resolve("test-mod")));

    instance.call();

    verify(fafService).uploadMod(any(), any());

    assertThat(Files.list(preferencesService.getCacheDirectory()).toArray(), emptyArray());
  }
}
