package com.faforever.client.login;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginControllerTest extends AbstractPlainJavaFxTest {
  private LoginController instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;

  @Before
  public void setUp() throws Exception {
    instance = new LoginController(userService, preferencesService);
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

    verify(userService).login("JUnit", "password", true);
  }
}
