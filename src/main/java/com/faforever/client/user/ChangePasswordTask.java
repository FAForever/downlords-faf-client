package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Validator;
import com.google.common.hash.Hashing;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChangePasswordTask extends CompletableTask<Void> implements InitializingBean {

  private final FafApiAccessor fafApiAccessor;
  private final I18n i18n;

  private String currentPassword;
  private String newPassword;
  private String username;

  @Inject
  public ChangePasswordTask(FafApiAccessor fafApiAccessor, I18n i18n) {
    super(Priority.HIGH);
    this.fafApiAccessor = fafApiAccessor;
    this.i18n = i18n;
  }

  @Override
  public void afterPropertiesSet() {
    updateTitle(i18n.get("settings.account.changePassword.changing"));
  }

  void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  void setUsername(String username) {
    this.username = username;
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(currentPassword, "currentPassword must not be null");
    Validator.notNull(newPassword, "newPassword must not be null");

    String currentPasswordHash = Hashing.sha256().hashString(currentPassword, UTF_8).toString();
    String newPasswordHash = Hashing.sha256().hashString(newPassword, UTF_8).toString();

    fafApiAccessor.changePassword(username, currentPasswordHash, newPasswordHash);

    return null;
  }
}
