package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Website;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends AbstractPlainJavaFxTest {
  private LoginController instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private PlatformService platformService;

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();

    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(preferencesService.getRemotePreferences()).thenReturn(CompletableFuture.failedFuture(new Exception(
        "Fall back to default configuration"
    )));

    instance = new LoginController(userService, preferencesService, platformService, clientProperties);

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
}
