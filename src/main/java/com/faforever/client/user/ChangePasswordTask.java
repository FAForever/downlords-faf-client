package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Validator;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChangePasswordTask extends CompletableTask<Void> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  I18n i18n;

  private String currentPassword;
  private String newPassword;

  public ChangePasswordTask() {
    super(Priority.HIGH);
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("settings.account.changePassword.changing"));
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(currentPassword, "currentPassword must not be null");
    Validator.notNull(newPassword, "newPassword must not be null");

    String currentPasswordHash = Hashing.sha256().hashString(currentPassword, UTF_8).toString();
    String newPasswordHash = Hashing.sha256().hashString(newPassword, UTF_8).toString();

    fafApiAccessor.changePassword(currentPasswordHash, newPasswordHash);

    return null;
  }
}
