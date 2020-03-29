package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Website;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.update.ClientConfiguration.Endpoints;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.update.DownloadUpdateTask;
import com.faforever.client.update.UpdateInfo;
import com.faforever.client.update.VersionTest;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends AbstractPlainJavaFxTest {
  public static final String LOGIN_WITH_EMAIL_WARNING_KEY = "login.withEmailWarning";

  private LoginController instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private PlatformService platformService;
  @Mock
  private I18n i18n;
  @Mock
  private ClientUpdateService clientUpdateService;

  private ClientProperties clientProperties;

  @Before
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();

    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(i18n.get(LOGIN_WITH_EMAIL_WARNING_KEY)).thenReturn(LOGIN_WITH_EMAIL_WARNING_KEY);

    instance = new LoginController(userService, preferencesService, platformService, clientProperties, i18n, clientUpdateService);

    Website website = clientProperties.getWebsite();
    website.setCreateAccountUrl("create");
    website.setForgotPasswordUrl("forgot");

    loadFxml("theme/login.fxml", param -> instance);
  }

  @Test
  public void testLoginNotCalledWhenNoUsernameAndPasswordSet() throws Exception {
    instance.display();

    verify(userService, never()).login(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testLoginButtonClicked() throws Exception {
    instance.usernameInput.setText("JUnit");
    instance.passwordInput.setText("password");
    instance.autoLoginCheckBox.setSelected(true);

    when(userService.login(anyString(), anyString(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onLoginButtonClicked();

    verify(userService).login("JUnit", "password", true);
  }

  @Test
  public void testCreateAccountButtton() throws Exception {
    instance.createNewAccountClicked();

    verify(platformService).showDocument("create");
  }

  @Test
  public void testForgotPasswordButtton() throws Exception {
    instance.forgotLoginClicked();

    verify(platformService).showDocument("forgot");
  }

  @Test
  public void testUsernameEmailWarning() {
    instance.usernameInput.setText("test@example.com");
    instance.passwordInput.setText("foo");
    instance.loginButton.fire();
    verify(i18n).get(LOGIN_WITH_EMAIL_WARNING_KEY);
    verifyZeroInteractions(userService);
    assertThat(instance.loginErrorLabel.isVisible(), is(true));
    assertThat(instance.loginErrorLabel.getText(), is(LOGIN_WITH_EMAIL_WARNING_KEY));
  }

  @Test
  public void testLoginSucceeds() {
    instance.usernameInput.setText("test");
    instance.passwordInput.setText("foo");
    instance.autoLoginCheckBox.setSelected(true);
    when(userService.login(eq("test"), eq("foo"), eq(true))).thenReturn(CompletableFuture.completedFuture(null));
    instance.loginButton.fire();
    assertThat(instance.loginErrorLabel.isVisible(), is(false));
  }

  @Test
  public void testInitializeWithNoMandatoryUpdate() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    ClientConfiguration.ReleaseInfo releaseInfo = new ReleaseInfo();
    ClientConfiguration.Endpoints endpoints = mock(Endpoints.class, Answers.RETURNS_DEEP_STUBS);
    clientConfiguration.setLatestRelease(releaseInfo);
    clientConfiguration.setEndpoints(Collections.singletonList(endpoints));

    releaseInfo.setMinimumVersion("2.1.2");
    VersionTest.setCurrentVersion("2.2.0");

    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    instance.initialize();
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    verify(clientUpdateService, atLeastOnce()).getNewestUpdate();
  }

  @Test
  public void testInitializeWithMandatoryUpdate() throws Exception {
    UpdateInfo updateInfo = new UpdateInfo(null, null, null, 5, null, false);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    ClientConfiguration.ReleaseInfo releaseInfo = new ReleaseInfo();
    ClientConfiguration.Endpoints endpoints = mock(Endpoints.class, Answers.RETURNS_DEEP_STUBS);
    clientConfiguration.setLatestRelease(releaseInfo);
    clientConfiguration.setEndpoints(Collections.singletonList(endpoints));

    releaseInfo.setMinimumVersion("2.1.2");
    VersionTest.setCurrentVersion("1.2.0");

    when(clientUpdateService.getNewestUpdate()).thenReturn(CompletableFuture.completedFuture(updateInfo));
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));

    clientProperties.setUseRemotePreferences(true);

    assertThat(instance.loginErrorLabel.isVisible(), is(false));
    assertThat(instance.downloadUpdateButton.isVisible(), is(false));
    assertThat(instance.loginFormPane.isDisable(), is(false));

    instance.initialize();
    WaitForAsyncUtils.waitForFxEvents();

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
  public void testOnLoginErrorIsRemoved() throws Exception {
    instance.loginErrorLabel.setVisible(true);
    when(userService.login(eq("username"), eq("password"), anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
    instance.usernameInput.setText("username");
    instance.passwordInput.setText("password");
    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.loginErrorLabel.isVisible());
  }

  @Test
  public void testWarningOnLoginWithEmail() throws Exception {
    instance.loginErrorLabel.setVisible(false);
    instance.usernameInput.setText("username@example.com");
    instance.passwordInput.setText("password");
    instance.onLoginButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verifyZeroInteractions(userService);
    verify(i18n).get("login.withEmailWarning");
    assertTrue(instance.loginErrorLabel.isVisible());
  }
}
