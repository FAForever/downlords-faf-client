package com.faforever.client.login;

import com.faforever.client.builders.ClientConfigurationBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
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
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.VersionTest;
import com.faforever.client.user.UserService;
import com.faforever.commons.api.dto.MeResult;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testfx.assertions.api.Assertions;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

  private static final URI REDIRECT_URI = URI.create("http://localhost");
  private static final URI EXPLICIT_REDIRECT_URI = URI.create("http://localhost/fallback");

  @InjectMocks
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
  private PlatformService platformService;
  @Mock
  private I18n i18n;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private AnnouncementController announcementController;
  @Mock
  private OfflineServiceController offlineServiceController;
  @Mock
  private OfflineServicesController offlineServicesController;
  @Mock
  private OAuthValuesReceiver oAuthValuesReceiver;

  @Spy
  private ClientProperties clientProperties = new ClientProperties();
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/login/announcement.fxml")).thenReturn(announcementController);
    when(uiService.loadFxml("theme/login/offline_service.fxml")).thenReturn(offlineServiceController);
    when(uiService.loadFxml("theme/login/offline_services.fxml")).thenReturn(offlineServicesController);
    when(statPingService.getServices()).thenReturn(Flux.empty());
    when(statPingService.getMessages()).thenReturn(Flux.empty());

    when(announcementController.getRoot()).thenReturn(new Pane());
    when(offlineServiceController.getRoot()).thenReturn(new Label());
    when(offlineServicesController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/login/login.fxml", param -> instance);
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());

    instance.oauthRedirectUriField.setText(EXPLICIT_REDIRECT_URI.toASCIIString());
  }

  @Test
  public void testLoginWithRefreshToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    String refreshToken = "asd";
    preferences.getLogin().setRefreshToken(refreshToken);
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken();
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginSucceeds() throws Exception {
    String state = "abc";
    String code = "asda";

    when(userService.getState()).thenReturn(state);
    when(userService.login(code, REDIRECT_URI)).thenReturn(CompletableFuture.completedFuture(null));
    when(oAuthValuesReceiver.receiveValues(List.of(EXPLICIT_REDIRECT_URI)))
        .thenReturn(CompletableFuture.completedFuture(new Values(code, state, REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(List.of(EXPLICIT_REDIRECT_URI));
    verify(userService).login(code, REDIRECT_URI);
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsWrongState() throws Exception {
    String state = "abc";
    String wrongState = "xyz";
    String code = "asda";

    when(userService.getState()).thenReturn(state);
    when(userService.login(code, REDIRECT_URI)).thenReturn(CompletableFuture.completedFuture(null));
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI))))
        .thenReturn(CompletableFuture.completedFuture(new Values(code, wrongState, REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(List.of(EXPLICIT_REDIRECT_URI));
    verify(userService, never()).login(code, REDIRECT_URI);
    verify(userService).getState();
    verify(notificationService).addImmediateErrorNotification(any(IllegalStateException.class), eq("login.failed"));
  }

  @Test
  public void testLoginFails() throws Exception {
    String state = "abc";
    String code = "asda";

    when(userService.getState()).thenReturn(state);
    when(userService.login(code, REDIRECT_URI)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));
    when(oAuthValuesReceiver.receiveValues(List.of(EXPLICIT_REDIRECT_URI)))
        .thenReturn(CompletableFuture.completedFuture(new Values(code, state, REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(userService).login(code, REDIRECT_URI);
    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsNoPorts() throws Exception {
    when(oAuthValuesReceiver.receiveValues(List.of(EXPLICIT_REDIRECT_URI)))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeout() throws Exception {
    when(oAuthValuesReceiver.receiveValues(List.of(EXPLICIT_REDIRECT_URI)))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateWarnNotification(eq("login.timeout"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeoutAlreadyLoggedIn() throws Exception {
    when(oAuthValuesReceiver.receiveValues(List.of(EXPLICIT_REDIRECT_URI)))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));
    when(userService.getOwnUser()).thenReturn(new MeResult());

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService, never()).addImmediateWarnNotification(eq("login.timeout"));
  }

  @Test
  public void testLoginRefreshFailsBadToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
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
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
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
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
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
  public void testInitializeWithMandatoryUpdateWithAutoLogin() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = ClientConfigurationBuilder.create()
        .defaultValues()
        .latestRelease()
        .minimumVersion("2.1.2")
        .then()
        .get();

    preferences.getLogin().setRememberMe(true);
    preferences.getLogin().setRefreshToken("abc");

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
    verify(userService, never()).loginWithRefreshToken();
    verify(userService, never()).login(anyString(), any());
  }

  @Test
  public void testInitializeWithMandatoryUpdateNoAutoLogin() throws Exception {
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
    verify(userService, never()).loginWithRefreshToken();
    verify(userService, never()).login(anyString(), any());
  }

  @Test
  public void testOnDownloadUpdateButtonClicked() {
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
    doThrow(new IOException()).when(gameService).startGameOffline();
    instance.onPlayOfflineButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(gameService).startGameOffline();
    verify(notificationService).addImmediateWarnNotification(eq("offline.noExe"));
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
