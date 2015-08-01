package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

  private UserServiceImpl userService;

  @Before
  public void setUp() throws Exception {
    userService = new UserServiceImpl();
    userService.lobbyServerAccessor = mock(LobbyServerAccessor.class);
    userService.preferencesService = mock(PreferencesService.class);

    when(userService.preferencesService.getPreferences()).thenReturn(new Preferences());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testLogin() throws Exception {
    Callback<Void> callback = mock(Callback.class);
    userService.login("junit", "junitPw", true, callback);

    verify(userService.lobbyServerAccessor).connectAndLogInInBackground(any(Callback.class));
    verify(userService.preferencesService).storeInBackground();

    assertEquals("junit", userService.getUsername());
    assertEquals("junitPw", userService.getPassword());
  }

  @Test
  public void testGetUsername() throws Exception {
    assertNull(userService.getUsername());
  }

  @Test
  public void testGetPassword() throws Exception {
    assertNull(userService.getPassword());
  }

  @Test
  public void testCancelLogin() throws Exception {
    userService.cancelLogin();

    verify(userService.lobbyServerAccessor).disconnect();
  }
}
