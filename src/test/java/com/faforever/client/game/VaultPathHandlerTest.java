package com.faforever.client.game;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.tasks.MoveDirectoryTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.UITest;
import javafx.scene.control.CheckBox;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class VaultPathHandlerTest extends UITest {

  @Mock
  private PlatformService platformService;
  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @InjectMocks
  private VaultPathHandler instance;

  @Test
  public void testOnVaultPathUpdated() {
    MoveDirectoryTask moveDirectoryTask = mock(MoveDirectoryTask.class);
    Path newVaultLocation = Path.of(".");
    when(applicationContext.getBean(MoveDirectoryTask.class)).thenReturn(moveDirectoryTask);

    instance.onVaultPathUpdated(newVaultLocation);

    verify(moveDirectoryTask).setOldDirectory(forgedAlliancePrefs.getVaultBaseDirectory());
    verify(moveDirectoryTask).setNewDirectory(newVaultLocation);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testVerifyPathAndShowWarning() {
    forgedAlliancePrefs.setWarnNonAsciiVaultPath(true);
    when(preferencesService.isVaultBasePathInvalidForAscii()).thenReturn(true);

    instance.verifyVaultPathAndShowWarning();

    verify(notificationService).addImmediateWarnNotification(any(), any(), any(), any(CheckBox.class));
  }

  @Test
  public void testVerifyPathAndDoNotShowWarning() {
    forgedAlliancePrefs.setWarnNonAsciiVaultPath(false);
    when(preferencesService.isVaultBasePathInvalidForAscii()).thenReturn(true);

    instance.verifyVaultPathAndShowWarning();

    verifyNoInteractions(notificationService);
  }
}