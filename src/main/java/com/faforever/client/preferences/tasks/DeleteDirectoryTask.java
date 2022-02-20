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
public class DeleteDirectoryTask extends CompletableTask<Void> {

  private final I18n i18n;
  private final NotificationService notificationService;

  @Setter
  private Path directory;

  @Inject
  public DeleteDirectoryTask(I18n i18n, NotificationService notificationService) {
    super(Priority.HIGH);
    this.i18n = i18n;
    this.notificationService = notificationService;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(directory, "Directory has not been set");
    updateTitle(i18n.get("directory.delete", directory));

    try {
      FileSystemUtils.deleteRecursively(directory);
    } catch (IOException e) {
      log.error("Could not delete directory {}", directory, e);
      notificationService.addImmediateErrorNotification(e, "directory.delete.failed", directory);
    }

    return null;
  }
}
