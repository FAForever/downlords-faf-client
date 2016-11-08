package com.faforever.client.user;

import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceImplTest {

  @Mock
  private Preferences preferences;
  @Mock
  private FafService fafService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LoginPrefs login;
  @Mock
  private EventBus eventBus;
  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;

  private UserServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new UserServiceImpl();
    instance.eventBus = eventBus;
    instance.fafService = fafService;
    instance.preferencesService = preferencesService;
    instance.taskService = taskService;
    instance.applicationContext = applicationContext;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getLogin()).thenReturn(login);
    when(login.setPassword(any())).thenReturn(login);
    when(login.setUsername(any())).thenReturn(login);
    when(login.setAutoLogin(anyBoolean())).thenReturn(login);
    when(applicationContext.getBean(ChangePasswordTask.class)).thenReturn(mock(ChangePasswordTask.class));
  }
//
//  @Test
//  public void testLogin() throws Exception {
//    SessionInfo sessionInfo = new SessionInfo();
//    sessionInfo.setSession(9871);
//    sessionInfo.setId(1234);
//    sessionInfo.setEmail("junit@example.com");
//
//    when(lobbyServerAccessor.connect()).thenReturn(CompletableFuture.completedFuture(sessionInfo));
//
//    instance.login("junit", "junitPw", true);
//
//    verify(login).setReceiver("junit");
//    verify(login).setPassword("junitPw");
//    verify(login).setAutoLogin(true);
//    verify(preferencesService).storeInBackground();
//    verify(lobbyServerAccessor).connect();
//
//    assertEquals("junit", instance.getUsername());
//    assertEquals("junitPw", instance.getPassword());
//    assertEquals("session", instance.getSessionId());
//    assertEquals(1234, instance.getUid());
//    assertEquals("junit@example.com", instance.getEmail());
//  }

  @Test
  public void testGetUsername() throws Exception {
    assertNull(instance.getUsername());
  }

  @Test
  public void testGetPassword() throws Exception {
    assertNull(instance.getPassword());
  }

  @Test
  public void testCancelLogin() throws Exception {
    instance.cancelLogin();

    verify(fafService).disconnect();
  }

  @Test
  public void testChangePassword() throws Exception {
    instance.changePassword("currentPasswordHash", "newPasswordHash");

    verify(taskService).submitTask(any(ChangePasswordTask.class));
  }
}
