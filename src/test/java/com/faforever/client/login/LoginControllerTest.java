package com.faforever.client.login;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends AbstractPlainJavaFxTest {
  @Value("${login.forgotLoginUrl}")
  String forgotLoginUrl;
  @Value("${login.createAccountUrl}")
  String createUrl;
  private LoginController instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private PlatformService platformService;

  @Before
  public void setUp() throws Exception {
    instance = new LoginController(userService, preferencesService, platformService, createUrl, forgotLoginUrl);
    loadFxml("theme/login.fxml", param -> instance);

    when(preferencesService.getPreferences()).thenReturn(new Preferences());
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

    instance.loginButtonClicked();

    verify(userService).login("JUnit", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", true);
  }
}
