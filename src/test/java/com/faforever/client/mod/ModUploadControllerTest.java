package com.faforever.client.mod;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ModUploadControllerTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder modFolder = new TemporaryFolder();

  private ModUploadController instance;

  @Mock
  private ModUploadTask modUploadTask;
  @Mock
  private ModService modService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ClientProperties cLientProperties;

  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafService fafService;

  @Before
  public void setUp() throws Exception {
    instance = new ModUploadController(modService, executorService, notificationService, reportingService, i18n, platformService, cLientProperties, eventBus);

    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(0)).run();
      return null;
    }).when(executorService).execute(any());

    modUploadTask = new ModUploadTask(preferencesService, fafService, i18n) {
      @Override
      protected Void call() {
        return null;
      }
    };

    loadFxml("theme/vault/mod/mod_upload.fxml", clazz -> instance);
  }

  @Test
  public void testSetModPath() {
    when(modService.extractModInfo(any())).thenReturn(new ModVersion());

    Path modPath = modFolder.getRoot().toPath();
    instance.setModPath(modPath);

    verify(modService, timeout(3000)).extractModInfo(modPath);
  }

  @Test
  public void testOnCancelUploadClicked() {
    when(modService.uploadMod(any())).thenReturn(modUploadTask);

    modUploadTask.getFuture().complete(null);

    instance.rulesCheckBox.setSelected(true);
    instance.onUploadClicked();
    instance.onCancelUploadClicked();

    assertThat(modUploadTask.isCancelled(), is(true));
  }

  @Test
  public void testOnUploadClicked() {
    when(modService.uploadMod(any())).thenReturn(modUploadTask);
    modUploadTask.getFuture().complete(null);

    instance.rulesCheckBox.setSelected(true);
    instance.onUploadClicked();

    verify(modService).uploadMod(any());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modUploadRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testNoUploadIfRulesNotChecked() {
    instance.onUploadClicked();
    verifyNoMoreInteractions(modService);
    assertThat(instance.rulesLabel.getStyleClass().contains("bad"), Matchers.is(true));
  }
}
