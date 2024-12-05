package com.faforever.client.login;

import com.faforever.client.builders.ClientConfigurationBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.Version;
import com.faforever.client.user.LoginService;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends PlatformTest {

  public static final String CODE = "asda";
  private static final URI REDIRECT_URI = URI.create("http://localhost");

  @InjectMocks
  private LoginController instance;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();
  @Mock
  private GameRunner gameRunner;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LoginService loginService;
  @Mock
  private PlatformService platformService;
  @Mock
  private I18n i18n;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private OAuthValuesReceiver oAuthValuesReceiver;

  @Spy
  private ClientProperties clientProperties;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private LoginPrefs loginPrefs;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/login/login.fxml", param -> instance);
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginWithRefreshToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    String refreshToken = "asd";
    loginPrefs.setRefreshToken(refreshToken);
    runOnFxThreadAndWait(() -> reinitialize(instance));
    verify(loginService).loginWithRefreshToken();
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginSucceeds() throws Exception {
    when(loginService.login(anyString(), anyString(), any())).thenReturn(Mono.empty());
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString())).thenAnswer(
        invocation -> CompletableFuture.completedFuture(new Values(CODE, invocation.getArgument(0), REDIRECT_URI)));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(anyString(), anyString());
    verify(loginService).login(eq(CODE), anyString(), eq(REDIRECT_URI));
    assertTrue(instance.loginProgressPane.isVisible());
    assertFalse(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsWrongState() throws Exception {
    String wrongState = "a";

    when(oAuthValuesReceiver.receiveValues(anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(new Values(CODE, wrongState, REDIRECT_URI)));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(oAuthValuesReceiver).receiveValues(anyString(), anyString());
    verify(loginService, never()).login(anyString(), anyString(), any());
    verify(notificationService).addImmediateErrorNotification(any(IllegalStateException.class), eq("login.failed"));
  }

  @Test
  public void testLoginFails() throws Exception {
    when(loginService.login(eq(CODE), anyString(), eq(REDIRECT_URI))).thenReturn(Mono.error(new FakeTestException()));
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString())).thenAnswer(
        invocation -> CompletableFuture.completedFuture(new Values(CODE, invocation.getArgument(0), REDIRECT_URI)));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(loginService).login(eq(CODE), anyString(), eq(REDIRECT_URI));
    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsNoPorts() throws Exception {
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("")));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("login.failed"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsKnownError() throws Exception {
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new KnownLoginErrorException("", "login.known")));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("login.known"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeout() throws Exception {
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateWarnNotification(eq("login.timeout"));
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginFailsTimeoutAlreadyLoggedIn() throws Exception {
    when(oAuthValuesReceiver.receiveValues(anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new SocketTimeoutException()));
    when(loginService.getOwnUser()).thenReturn(new MeResult());
    when(loginService.getOwnPlayer()).thenReturn(new Player(0, "junit", null, null, "US", Map.of(), Map.of(), null));

    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService, never()).addImmediateWarnNotification(eq("login.timeout"));
  }

  @Test
  @Disabled("Flaky test on github actions")
  public void testLoginRefreshFailsBadToken() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    loginPrefs.setRememberMe(true);
    loginPrefs.setRefreshToken("abc");
    when(loginService.loginWithRefreshToken()).thenReturn(Mono.error(
        WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(), "", HttpHeaders.EMPTY, new byte[]{}, null)));
    runOnFxThreadAndWait(() -> reinitialize(instance));
    verify(loginService).loginWithRefreshToken();
    verify(notificationService, never()).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFailsUnauthorized() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    loginPrefs.setRememberMe(true);
    loginPrefs.setRefreshToken("abc");
    when(loginService.loginWithRefreshToken()).thenReturn(Mono.error(
        WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "", HttpHeaders.EMPTY, new byte[]{}, null)));
    runOnFxThreadAndWait(() -> reinitialize(instance));
    verify(loginService).loginWithRefreshToken();
    verify(notificationService).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFails() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync())
        .thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    loginPrefs.setRememberMe(true);
    loginPrefs.setRefreshToken("abc");
    when(loginService.loginWithRefreshToken()).thenReturn(Mono.error(new Exception("")));
    runOnFxThreadAndWait(() -> reinitialize(instance));
    verify(loginService).loginWithRefreshToken();
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

    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> {
      try (MockedStatic<Version> mockedVersion = mockStatic(Version.class)) {
        mockedVersion.when(Version::getCurrentVersion).thenReturn("2.2.0");
        mockedVersion.when(() -> Version.shouldUpdate(anyString(), anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.removePrefix(anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.followsSemverPattern(anyString())).thenCallRealMethod();
        reinitialize(instance);
      }
    });

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

    loginPrefs.setRememberMe(true);
    loginPrefs.setRefreshToken("abc");

    when(operatingSystem.supportsUpdateInstall()).thenReturn(supportsUpdateInstall);
    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> {
      try (MockedStatic<Version> mockedVersion = mockStatic(Version.class)) {
        mockedVersion.when(Version::getCurrentVersion).thenReturn("1.2.0");
        mockedVersion.when(() -> Version.shouldUpdate(anyString(), anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.removePrefix(anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.followsSemverPattern(anyString())).thenCallRealMethod();
        reinitialize(instance);
      }
    });

    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.downloadUpdateButton.isVisible(), is(supportsUpdateInstall));
    assertThat(instance.loginFormPane.isDisable(), is(true));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
    verify(i18n).get("login.clientTooOldError", "1.2.0", "2.1.2");
    verify(loginService, never()).loginWithRefreshToken();
    verify(loginService, never()).login(anyString(), anyString(), any());
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

    when(operatingSystem.supportsUpdateInstall()).thenReturn(supportsUpdateInstall);
    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    runOnFxThreadAndWait(() -> {
      try (MockedStatic<Version> mockedVersion = mockStatic(Version.class)) {
        mockedVersion.when(Version::getCurrentVersion).thenReturn("1.2.0");
        mockedVersion.when(() -> Version.shouldUpdate(anyString(), anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.removePrefix(anyString())).thenCallRealMethod();
        mockedVersion.when(() -> Version.followsSemverPattern(anyString())).thenCallRealMethod();
        reinitialize(instance);
      }
    });

    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.downloadUpdateButton.isVisible(), is(supportsUpdateInstall));
    assertThat(instance.loginFormPane.isDisable(), is(true));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
    verify(i18n).get("login.clientTooOldError", "1.2.0", "2.1.2");
    verify(loginService, never()).loginWithRefreshToken();
    verify(loginService, never()).login(anyString(), anyString(), any());
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

    verify(gameRunner).startOffline();
  }
}
