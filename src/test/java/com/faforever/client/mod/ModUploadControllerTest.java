package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
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
  private ThreadPoolExecutor threadPoolExecutor;

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
    instance = new ModUploadController(modService, threadPoolExecutor, notificationService, reportingService, i18n, eventBus);

    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(0)).run();
      return null;
    }).when(threadPoolExecutor).execute(any());

    modUploadTask = new ModUploadTask(preferencesService, fafService, i18n) {
      @Override
      protected Void call() throws Exception {
        return null;
      }
    };

    loadFxml("theme/vault/mod/mod_upload.fxml", clazz -> instance);
  }

  @Test
  public void testSetModPath() throws Exception {
    when(modService.extractModInfo(any())).thenReturn(new Mod());

    Path modPath = modFolder.getRoot().toPath();
    instance.setModPath(modPath);

    verify(modService, timeout(3000)).extractModInfo(modPath);
  }

  @Test
  public void testOnCancelUploadClicked() throws Exception {
    when(modService.uploadMod(any())).thenReturn(modUploadTask);

    modUploadTask.getFuture().complete(null);

    instance.onUploadClicked();
    instance.onCancelUploadClicked();

    assertThat(modUploadTask.isCancelled(), is(true));
  }

  @Test
  public void testOnUploadClicked() throws Exception {
    when(modService.uploadMod(any())).thenReturn(modUploadTask);
    modUploadTask.getFuture().complete(null);

    instance.onUploadClicked();

    verify(modService).uploadMod(any());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modUploadRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
