package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientConfigurationBuilder;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.VersionTest;
import com.faforever.client.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends AbstractPlainJavaFxTest {

  private LoginController instance;
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
  private WebViewConfigurer webViewConfigurer;

  private ClientProperties clientProperties;
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(userService.getHydraUrl()).thenReturn("google.com");

    instance = new LoginController(userService, preferencesService, notificationService, platformService, clientProperties, i18n, clientUpdateService, webViewConfigurer);

    loadFxml("theme/login.fxml", param -> instance);
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
    verify(userService).loginWithRefreshToken(refreshToken);
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
    String refreshToken = "asd";
    preferences.getLogin().setRefreshToken(refreshToken);
    when(userService.loginWithRefreshToken(refreshToken)).thenReturn(CompletableFuture.failedFuture(
        new CompletionException(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", HttpHeaders.EMPTY, new byte[]{}, null))));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken(refreshToken);
    verify(notificationService, never()).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFailsUnauthorized() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    String refreshToken = "asd";
    preferences.getLogin().setRefreshToken(refreshToken);
    when(userService.loginWithRefreshToken(refreshToken)).thenReturn(CompletableFuture.failedFuture(
        new CompletionException(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, new byte[]{}, null))));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken(refreshToken);
    verify(notificationService, never()).addImmediateErrorNotification(any(), anyString());
    assertFalse(instance.loginProgressPane.isVisible());
    assertTrue(instance.loginFormPane.isVisible());
  }

  @Test
  public void testLoginRefreshFails() {
    clientProperties.setUseRemotePreferences(true);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(ClientConfigurationBuilder.create().defaultValues().get()));
    String refreshToken = "asd";
    preferences.getLogin().setRefreshToken(refreshToken);
    when(userService.loginWithRefreshToken(refreshToken)).thenReturn(CompletableFuture.failedFuture(new CompletionException(new Exception())));
    runOnFxThreadAndWait(() -> instance.initialize());
    verify(userService).loginWithRefreshToken(refreshToken);
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
  public void testSeeServerStatus() {
    clientProperties.setStatusPageUrl(null);
    instance.seeServerStatus();

    verify(platformService, never()).showDocument(anyString());

    clientProperties.setStatusPageUrl("");
    instance.seeServerStatus();

    verify(platformService).showDocument(anyString());
  }
}
