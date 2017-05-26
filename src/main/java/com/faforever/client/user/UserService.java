package com.faforever.client.user;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
public class UserService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StringProperty username;

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;

  private String password;
  private Integer userId;
  private CompletableFuture<Void> loginFuture;

  @Inject
  public UserService(FafService fafService, PreferencesService preferencesService, EventBus eventBus, ApplicationContext applicationContext, TaskService taskService) {
    username = new SimpleStringProperty();
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;
    this.applicationContext = applicationContext;
    this.taskService = taskService;
  }


  public CompletableFuture<Void> login(String username, String password, boolean autoLogin) {
    this.password = password;

    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);
    preferencesService.storeInBackground();

    loginFuture = fafService.connectAndLogIn(username, password)
        .thenAccept(loginInfo -> {
          userId = loginInfo.getId();

          // Because of different case (upper/lower)
          String login = loginInfo.getLogin();
          UserService.this.username.set(login);

          preferencesService.getPreferences().getLogin().setUsername(login);
          preferencesService.storeInBackground();

          eventBus.post(new LoginSuccessEvent(login, password, userId));
        })
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            logger.warn("Error during login", throwable);
            fafService.disconnect();
          }
          loginFuture = null;
        });
    return loginFuture;
  }


  public String getUsername() {
    return username.get();
  }


  public String getPassword() {
    return password;
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
    preferencesService.getPreferences().getLogin().setAutoLogin(false);
  }


  public CompletableTask<Void> changePassword(String currentPassword, String newPassword) {
    ChangePasswordTask changePasswordTask = applicationContext.getBean(ChangePasswordTask.class);
    changePasswordTask.setUsername(username.get());
    changePasswordTask.setCurrentPassword(currentPassword);
    changePasswordTask.setNewPassword(newPassword);

    return taskService.submitTask(changePasswordTask);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(LoginMessage.class, loginInfo -> userId = loginInfo.getId());
    fafService.addOnMessageListener(NoticeMessage.class, this::onLoginError);
    eventBus.register(this);
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }
}
