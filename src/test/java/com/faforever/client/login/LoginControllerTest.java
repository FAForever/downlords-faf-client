package com.faforever.client.login;

import com.faforever.client.builders.ClientConfigurationBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.VersionTest;
import com.faforever.client.user.UserService;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
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

  public static final String CODE = "asda";
  private static final URI REDIRECT_URI = URI.create("http://localhost");
  private static final URI EXPLICIT_REDIRECT_URI = URI.create("http://localhost/fallback");

  @InjectMocks
  private LoginController instance;
  @Mock
  private OperatingSystem operatingSystem;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;
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
    when(userService.login(eq(CODE), anyString(), eq(REDIRECT_URI))).thenReturn(CompletableFuture.completedFuture(null));
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenAnswer(invocation -> CompletableFuture.completedFuture(new Values(CODE, invocation.getArgument(1), REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString());
    verify(userService).login(eq(CODE), anyString(), eq(REDIRECT_URI));
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsWrongState() throws Exception {
    String wrongState = "a";

    when(userService.login(eq(CODE), anyString(), eq(REDIRECT_URI))).thenReturn(CompletableFuture.completedFuture(null));
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(new Values(CODE, wrongState, REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString());
    verify(userService, never()).login(anyString(), anyString(), any());
    verify(notificationService).addImmediateErrorNotification(any(IllegalStateException.class), eq("login.failed"));
  }

  @Test
  public void testLoginFails() throws Exception {
    when(userService.login(eq(CODE), anyString(), eq(REDIRECT_URI))).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenAnswer(invocation -> CompletableFuture.completedFuture(new Values(CODE, invocation.getArgument(1), REDIRECT_URI)));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(userService).login(eq(CODE), anyString(), eq(REDIRECT_URI));
    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsNoPorts() throws Exception {
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeout() throws Exception {
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));

    instance.onLoginButtonClicked().get();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateWarnNotification(eq("login.timeout"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeoutAlreadyLoggedIn() throws Exception {
    when(oAuthValuesReceiver.receiveValues(eq(List.of(EXPLICIT_REDIRECT_URI)), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));
    when(userService.getOwnUser()).thenReturn(new MeResult());
    when(userService.getOwnPlayer()).thenReturn(new Player(0, "junit", null, null, "US", Map.of(), Map.of()));

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


  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testInitializeWithMandatoryUpdateWithAutoLogin(boolean supportsUpdateInstall) throws Exception {
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

    when(operatingSystem.supportsUpdateInstall()).thenReturn(supportsUpdateInstall);
    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.downloadUpdateButton.isVisible(), is(supportsUpdateInstall));
    assertThat(instance.loginFormPane.isDisable(), is(true));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
    verify(i18n).get("login.clientTooOldError", "1.2.0", "2.1.2");
    verify(userService, never()).loginWithRefreshToken();
    verify(userService, never()).login(anyString(), anyString(), any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testInitializeWithMandatoryUpdateNoAutoLogin(boolean supportsUpdateInstall) throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = ClientConfigurationBuilder.create()
        .defaultValues()
        .latestRelease()
        .minimumVersion("2.1.2")
        .then()
        .get();

    VersionTest.setCurrentVersion("1.2.0");

    when(operatingSystem.supportsUpdateInstall()).thenReturn(supportsUpdateInstall);
    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> instance.initialize());

    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.downloadUpdateButton.isVisible(), is(supportsUpdateInstall));
    assertThat(instance.loginFormPane.isDisable(), is(true));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
    verify(i18n).get("login.clientTooOldError", "1.2.0", "2.1.2");
    verify(userService, never()).loginWithRefreshToken();
    verify(userService, never()).login(anyString(), anyString(), any());
  }

  @Test
  public void testOnDownloadUpdateButtonClicked() {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    DownloadUpdateTask downloadUpdateTask = new DownloadUpdateTask(i18n, dataPrefs);
    when(clientUpdateService.downloadAndInstallInBackground(updateInfo)).thenReturn(downloadUpdateTask);

    ReflectionTestUtils.setField(instance, "updateInfoFuture", CompletableFuture.completedFuture(updateInfo));

    instance.onDownloadUpdateButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n, atLeastOnce()).get("login.button.downloadPreparing");
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
}
