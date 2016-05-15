package com.faforever.client.mod;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UploadModControllerTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder modFolder = new TemporaryFolder();
  private UploadModController instance;
  @Mock
  private ModService modService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private UploadModTask uploadModTask;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;

  @Before
  public void setUp() throws Exception {
    instance = loadController("upload_mod.fxml");
    instance.modService = modService;
    instance.notificationService = notificationService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.reportingService = reportingService;
  }

  @Test
  public void testSetModPath() throws Exception {
    when(modService.extractModInfo(any())).thenReturn(new ModInfoBean());
    instance.setModPath(modFolder.getRoot().toPath());
  }

  @Test
  public void testOnCancelUploadClicked() throws Exception {
    when(modService.uploadMod(any(), any())).thenReturn(uploadModTask);
    when(uploadModTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUploadClicked();
    instance.onCancelUploadClicked();

    verify(uploadModTask).cancel(true);
  }

  @Test
  public void testOnUploadClicked() throws Exception {
    when(modService.uploadMod(any(), any())).thenReturn(uploadModTask);
    when(uploadModTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUploadClicked();

    verify(modService).uploadMod(any(), any());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modUploadRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
