package com.faforever.client.login;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.status.Message;
import com.faforever.client.status.Service;
import com.faforever.client.status.StatPingService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientConfigurationBuilder;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.VersionTest;
import com.faforever.client.user.UserService;
import com.github.nocatch.NoCatchException;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testfx.assertions.api.Assertions;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends UITest {

  private LoginController instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;
  @Mock
  private StatPingService statPingService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private AnnouncementController announcementController;
  @Mock
  private OfflineServiceController offlineServiceController;
  @Mock
  private OfflineServicesController offlineServicesController;

  private ClientProperties clientProperties;
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(userService.getHydraUrl()).thenReturn("google.com");
    when(uiService.loadFxml("theme/login/announcement.fxml")).thenReturn(announcementController);
    when(uiService.loadFxml("theme/login/offline_service.fxml")).thenReturn(offlineServiceController);
    when(uiService.loadFxml("theme/login/offline_services.fxml")).thenReturn(offlineServicesController);
    when(statPingService.getServices()).thenReturn(Flux.empty());
    when(statPingService.getMessages()).thenReturn(Flux.empty());

    when(announcementController.getRoot()).thenReturn(new Pane());
    when(offlineServiceController.getRoot()).thenReturn(new Label());
    when(offlineServicesController.getRoot()).thenReturn(new Pane());

    instance = new LoginController(gameService, userService, preferencesService, notificationService, clientProperties, i18n,
        clientUpdateService, webViewConfigurer, statPingService, uiService);

    loadFxml("theme/login/login.fxml", param -> instance);
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginWithRefreshToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    String refreshToken = "asd";
    preferences.getLogin().setRefreshToken(refreshToken);
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken();
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginSucceeds() {
    String state = "abc";
    String code = "asda";
    when(userService.getState()).thenReturn(state);
    runOnFxThreadAndWait(() -> instance.loginWebView.getEngine().load(String.format("?code=%s&state=%s", code, state)));
    verify(userService).login(code);
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsWrongState() {
    String state = "abc";
    String wrongState = "xyz";
    String code = "asda";
    when(userService.getState()).thenReturn(state);
    runOnFxThreadAndWait(() -> instance.loginWebView.getEngine().load(String.format("?code=%s&state=%s", code, wrongState)));
    verify(userService, never()).login(code);
    verify(userService).getState();
    verify(userService).getHydraUrl();
    assertEquals(userService.getHydraUrl(), instance.loginWebView.getEngine().getLocation());
    verify(notificationService).addImmediateErrorNotification(any(IllegalStateException.class), eq("login.badState"));
  }

  @Test
  public void testLoginFails() {
    String state = "abc";
    String code = "asda";
    when(userService.getState()).thenReturn(state);
    when(userService.login(code)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));
    runOnFxThreadAndWait(() -> instance.loginWebView.getEngine().load(String.format("?code=%s&state=%s", code, state)));
    verify(userService).login(code);
    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginError() {
    runOnFxThreadAndWait(() -> instance.loginWebView.getEngine().load("?error=abc&error_description=def"));
    verify(userService, never()).login(anyString());
    verify(userService).getHydraUrl();
    assertEquals(userService.getHydraUrl(), instance.loginWebView.getEngine().getLocation());
    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), eq("login.error"), eq("abc"), eq("def"));
  }

  @Test
  public void testLoginRefreshFailsBadToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    preferences.getLogin().setRememberMe(true);
    preferences.getLogin().setRefreshToken("abc");
    when(userService.loginWithRefreshToken()).thenReturn(CompletableFuture.failedFuture(
        new CompletionException(WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(), "", HttpHeaders.EMPTY, new byte[]{}, null))));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken();
    verify(notificationService, never()).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFailsUnauthorized() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    preferences.getLogin().setRememberMe(true);
    preferences.getLogin().setRefreshToken("abc");
    when(userService.loginWithRefreshToken()).thenReturn(CompletableFuture.failedFuture(
        new CompletionException(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "", HttpHeaders.EMPTY, new byte[]{}, null))));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken();
    verify(notificationService, never()).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFails() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    preferences.getLogin().setRememberMe(true);
    preferences.getLogin().setRefreshToken("abc");
    when(userService.loginWithRefreshToken()).thenReturn(CompletableFuture.failedFuture(new CompletionException(new Exception())));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken();
    verify(notificationService).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testInitializeWithNoMandatoryUpdate() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = ClientConfigurationBuilder.create()
        .defaultValues()
        .latestRelease()
        .minimumVersion("2.1.2")
        .then()
        .get();

    VersionTest.setCurrentVersion("2.2.0");

    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
  }

  @Test
  public void testInitializeWithMandatoryUpdate() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = ClientConfigurationBuilder.create()
        .defaultValues()
        .latestRelease()
        .minimumVersion("2.1.2")
        .then()
        .get();

    VersionTest.setCurrentVersion("1.2.0");

    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.downloadUpdateButton.isVisible(), is(true));
    assertThat(instance.loginFormPane.isDisable(), is(true));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
    verify(i18n).get("login.clientTooOldError", "1.2.0", "2.1.2");
  }

  @Test
  public void testOnDownloadUpdateButtonClicked() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    DownloadUpdateTask downloadUpdateTask = new DownloadUpdateTask(i18n, preferencesService);
    when(clientUpdateService.downloadAndInstallInBackground(updateInfo)).thenReturn(downloadUpdateTask);

    ReflectionTestUtils.setField(instance, "updateInfoFuture", CompletableFuture.completedFuture(updateInfo));

    instance.onDownloadUpdateButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n).get("login.button.downloadPreparing");
    verify(clientUpdateService, atLeastOnce()).downloadAndInstallInBackground(updateInfo);
  }

  @Test
  public void testOnPlayOfflineButtonClicked() throws Exception {
    instance.onPlayOfflineButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(gameService).startGameOffline();
  }

  @Test
  public void testOnPlayOfflineButtonClickedNoExe() throws Exception {
    doThrow(new NoCatchException(new IOException())).when(gameService).startGameOffline();
    instance.onPlayOfflineButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(gameService).startGameOffline();
    verify(notificationService).addImmediateWarnNotification("offline.noExe");
  }

  @Test
  public void testOnPlayOfflineButtonClickedError() throws Exception {
    doThrow(new RuntimeException()).when(gameService).startGameOffline();
    instance.onPlayOfflineButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(gameService).startGameOffline();
    verify(notificationService).addImmediateErrorNotification(any(), eq("offline.error"));
  }

  @Test
  void testInitializeWithActiveAnnouncement_displayed() {
    when(statPingService.getMessages()).thenReturn(Flux.just(new Message(
        1,
        "JUnit",
        "Description",
        OffsetDateTime.now(),
        OffsetDateTime.now().plusHours(1),
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )));

    assertThat(instance.messagesContainer.getChildren(), hasSize(0));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.messagesContainer.getChildren(), hasSize(1));
    verify(uiService).loadFxml("theme/login/announcement.fxml");
  }

  @Test
  void testInitializeWithNoAnnouncement_notDisplayed() {
    assertThat(instance.messagesContainer.getChildren(), hasSize(0));
  }

  @Test
  void testInitializeWithFutureAnnouncement_displayed() {
    when(statPingService.getMessages()).thenReturn(Flux.just(new Message(
        1,
        "JUnit",
        "Description",
        OffsetDateTime.now().plusDays(1),
        OffsetDateTime.now().plusDays(2),
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )));

    assertThat(instance.messagesContainer.getChildren(), hasSize(0));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.messagesContainer.getChildren(), hasSize(1));
    verify(uiService).loadFxml("theme/login/announcement.fxml");
  }

  @Test
  void testInitializeWithFutureAnnouncementAndServiceOffline_bothDisplayed() {
    when(statPingService.getMessages()).thenReturn(Flux.just(new Message(
        1,
        "JUnit",
        "Description",
        OffsetDateTime.now().plusDays(1),
        OffsetDateTime.now().plusDays(2),
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )));
    when(statPingService.getServices()).thenReturn(Flux.just(new Service(
        1,
        Collections.emptyList(),
        OffsetDateTime.now().minusMinutes(1),
        OffsetDateTime.now().minusHours(1),
        Collections.emptyList(),
        "Lobby",
        false,
        "lobby"
    )));

    assertThat(instance.messagesContainer.getChildren(), hasSize(0));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.messagesContainer.getChildren(), hasSize(2));
    verify(uiService).loadFxml("theme/login/announcement.fxml");
    verify(uiService).loadFxml("theme/login/offline_services.fxml");
  }

  @Test
  void testInitializeWithActiveAnnouncementAndServiceOffline_serviceNotDisplayed() {
    when(statPingService.getMessages()).thenReturn(Flux.just(new Message(
        1,
        "JUnit",
        "Description",
        OffsetDateTime.now().minusMinutes(1),
        OffsetDateTime.now().plusHours(1),
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )));
    when(statPingService.getServices()).thenReturn(Flux.just(new Service(
        1,
        Collections.emptyList(),
        OffsetDateTime.now().minusMinutes(1),
        OffsetDateTime.now().minusHours(1),
        Collections.emptyList(),
        "Lobby",
        false,
        "lobby"
    )));

    Assertions.assertThat(instance.messagesContainer).hasNoChildren();

    runOnFxThreadAndWait(() -> instance.initialize());

    Assertions.assertThat(instance.messagesContainer).hasExactlyNumChildren(1);
    verify(uiService).loadFxml("theme/login/announcement.fxml");
  }
}
