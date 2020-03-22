package com.faforever.client.user;

import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.dto.MeResult;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.LoginPrefs.RememberMeType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UserAndRefreshToken;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
@RequiredArgsConstructor
public class UserService implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ObjectProperty<MeResult> ownUser = new SimpleObjectProperty<>();

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;

  private Integer userId;
  private CompletableFuture<Void> loginFuture;


  public CompletableFuture<Void> login(String username, String password, String refreshToken, RememberMeType rememberMeType) {
    CompletableFuture<UserAndRefreshToken> loginFutureWithCredentials;

    if (rememberMeType == RememberMeType.SHORT && refreshToken != null) {
      loginFutureWithCredentials = fafService.logIn(refreshToken);
    } else {
      loginFutureWithCredentials = fafService.logIn(username, password);
    }

    loginFuture = loginFutureWithCredentials
        .thenApply(userAndRefreshToken -> {
          ownUser.set(userAndRefreshToken.getUser());
          // Because of different case (upper/lower)
          preferencesService.getPreferences().getLogin().setUsername(username);
          if (rememberMeType == RememberMeType.SHORT) {
            preferencesService.getPreferences().getLogin().setRefreshToken(userAndRefreshToken.getRefreshToken());
            preferencesService.getPreferences().getLogin().setPassword(null);
          } else if (rememberMeType == RememberMeType.LONG) {
            preferencesService.getPreferences().getLogin().setPassword(password);
            preferencesService.getPreferences().getLogin().setRefreshToken(null);
          } else {
            preferencesService.getPreferences().getLogin().setPassword(null);
            preferencesService.getPreferences().getLogin().setRefreshToken(null);
          }
          preferencesService.getPreferences().getLogin().setRememberMeType(rememberMeType);
          preferencesService.storeInBackground();
          return userAndRefreshToken;
        })
        .thenCompose(fafService::connectServer)
        .thenAccept(userAndRefreshToken -> eventBus.post(new LoginSuccessEvent(userAndRefreshToken.getUser())))
        .whenComplete((aVoid, throwable) -> loginFuture = null);
    return loginFuture;
  }


  public String getUsername() {
    return ownUser.get().getUserName();
  }

  public Integer getUserId() {
    return userId;
  }


  public void cancelLogin() {
    if (loginFuture != null) {
      loginFuture.toCompletableFuture().cancel(true);
      loginFuture = null;
      fafService.disconnect();
    }
  }

  private void onLoginError(NoticeMessage noticeMessage) {
    if (loginFuture != null) {
      loginFuture.toCompletableFuture().completeExceptionally(new LoginFailedException(noticeMessage.getText()));
      loginFuture = null;
      fafService.disconnect();
    }
  }

  public void logOut() {
    logger.info("Logging out");
    fafService.disconnect();
    eventBus.post(new LoggedOutEvent());
    preferencesService.getPreferences().getLogin().setRememberMeType(RememberMeType.NEVER);
  }

  @EventListener
  public void onSessionExpired(SessionExpiredEvent sessionExpiredEvent) {
    if (loginFuture.isDone()) {
      logOut();
      notificationService.addNotification(new ImmediateNotification(i18n.get("session.expired.title"), i18n.get("session.expired.message"), Severity.INFO));
    }
  }

  public CompletableTask<Void> changePassword(String currentPassword, String newPassword) {
    ChangePasswordTask changePasswordTask = applicationContext.getBean(ChangePasswordTask.class);
    changePasswordTask.setUsername(ownUser.get().getUserName());
    changePasswordTask.setCurrentPassword(currentPassword);
    changePasswordTask.setNewPassword(newPassword);

    return taskService.submitTask(changePasswordTask);
  }

  @Override
  public void afterPropertiesSet() {
    fafService.addOnMessageListener(LoginMessage.class, loginInfo -> userId = loginInfo.getId());
    fafService.addOnMessageListener(NoticeMessage.class, this::onLoginError);
    eventBus.register(this);
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }

  public MeResult getOwnUser() {
    return ownUser.get();
  }

  public void setOwnUser(MeResult ownUser) {
    this.ownUser.set(ownUser);
  }

  public ObjectProperty<MeResult> ownUserProperty() {
    return ownUser;
  }
}
