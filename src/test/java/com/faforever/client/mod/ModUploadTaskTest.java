package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModUploadTaskTest extends UITest {

  @TempDir
  public Path tempDirectory;
  @InjectMocks
  private ModUploadTask instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private I18n i18n;
  @Spy
  private DataPrefs dataPrefs;

  @BeforeEach
  public void setUp() throws Exception {
    dataPrefs.setBaseDataDirectory(tempDirectory);

    Files.createDirectories(dataPrefs.getCacheDirectory());
    when(i18n.get(any())).thenReturn("");
    when(fafApiAccessor.uploadFile(any(), any(), any(), any())).thenReturn(Mono.empty());
  }

  @Test
  public void testModPathNull() throws Exception {
    assertThrows(NullPointerException.class, () -> instance.call());
  }

  @Test
  public void testProgressListenerNull() throws Exception {
    instance.setModPath(Path.of("."));
    assertThrows(NullPointerException.class, () -> instance.call());
  }

  @Test
  public void testCall() throws Exception {
    instance.setModPath(Files.createDirectories(tempDirectory.resolve("test-mod")));

    instance.call();

    verify(fafApiAccessor).uploadFile(any(), any(), any(), any());

    assertThat(Files.list(dataPrefs.getCacheDirectory()).toArray(), emptyArray());
  }
}
