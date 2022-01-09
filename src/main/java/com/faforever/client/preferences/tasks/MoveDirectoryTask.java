package com.faforever.client.preferences.tasks;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.task.CompletableTask;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MoveDirectoryTask extends CompletableTask<Void> {

  private final I18n i18n;
  private final NotificationService notificationService;

  @Setter
  private Path oldDirectory;
  @Setter
  private Path newDirectory;
  @Setter
  private boolean preserveOldDirectory;
  @Setter
  private Runnable afterCopyAction = () -> {};

  @Inject
  public MoveDirectoryTask(I18n i18n, NotificationService notificationService) {
    super(Priority.HIGH);
    this.i18n = i18n;
    this.notificationService = notificationService;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(oldDirectory, "Old directory has not been set");
    Objects.requireNonNull(newDirectory, "New directory has not been set");
    updateTitle(i18n.get("directory.move", oldDirectory, newDirectory));

    try {
      FileSystemUtils.copyRecursively(oldDirectory, newDirectory);
    } catch (IOException e) {
      FileSystemUtils.deleteRecursively(newDirectory);
      log.warn("Could not copy files to new directory {}", newDirectory, e);
      notificationService.addImmediateErrorNotification(e, "directory.move.failed", oldDirectory, newDirectory);
      return null;
    }

    afterCopyAction.run();

    if (!preserveOldDirectory) {
      updateTitle(i18n.get("directory.delete", oldDirectory));
      try {
        FileSystemUtils.deleteRecursively(oldDirectory);
      } catch (IOException e) {
        log.warn("Could not delete files from old directory {}", oldDirectory, e);
        notificationService.addImmediateErrorNotification(e, "directory.delete.failed", oldDirectory);
      }
    }

    return null;
  }
}
