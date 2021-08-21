package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.Player;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest extends ServiceTest {

  public static final String BASE_URL = "hydra";
  public static final String CLIENT_ID = "test";
  public static final String REDIRECT_URL = "localhost";
  public static final String SCOPES = "scope";

  @Mock
  private ClientProperties clientProperties;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private EventBus eventBus;
  @Mock
  private TokenService tokenService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  private UserService instance;
  private Preferences preferences;
  private LoginSuccessResponse validLoginMessage;
  private MeResult meResult;

  @BeforeEach
  public void setUp() throws Exception {
    Player me = new Player(1, "junit", null, null, "", new HashMap<>(), new HashMap<>());
    validLoginMessage = new LoginSuccessResponse(me);
    meResult = new MeResult();
    meResult.setUserName("junit");
    meResult.setUserId("1");
    MockitoAnnotations.initMocks(this);
    Oauth oauth = new Oauth();
    oauth.setBaseUrl(BASE_URL);
    oauth.setClientId(CLIENT_ID);
    oauth.setRedirectUrl(REDIRECT_URL);
    oauth.setScopes(SCOPES);
    when(clientProperties.getOauth()).thenReturn(oauth);

    instance = new UserService(clientProperties, fafServerAccessor, fafApiAccessor, preferencesService, eventBus, tokenService, notificationService, i18n);
    instance.afterPropertiesSet();
    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    verify(eventBus).register(instance);
  }

  @Test
  public void testGetHydraUrl() {
    String url = instance.getHydraUrl();
    assertTrue(url.contains(BASE_URL));
    assertTrue(url.contains(CLIENT_ID));
    assertTrue(url.contains(REDIRECT_URL));
    assertTrue(url.contains(SCOPES));
    assertNotNull(instance.getState());
  }

  @Test
  public void testLogin() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testReLoginWhenConnected() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor, never()).connectAndLogIn();
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraCodeError() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(tokenService).loginWithAuthorizationCode(anyString());

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor, never()).authorize();
    verify(fafServerAccessor, never()).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiAuthorizeError() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(fafApiAccessor).authorize();

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor, never()).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiVerifyError() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor, never()).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginLobbyError() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.failedFuture(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWrongUserFromServer() {
    Player notMe = new Player(100, "notMe", null, null, "", new HashMap<>(), new HashMap<>());
    LoginSuccessResponse invalidLoginMessage = new LoginSuccessResponse(notMe);
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(invalidLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.failedFuture(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginAfterExpiredWithDifferentUser() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    MeResult otherResult = new MeResult();
    otherResult.setUserName("junit2");
    otherResult.setUserId("2");
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(otherResult));
    Player me = new Player(2, "junit2", null, null, "", new HashMap<>(), new HashMap<>());
    LoginSuccessResponse loginMessage = new LoginSuccessResponse(me);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(loginMessage));

    instance.login("abc").join();

    verify(fafServerAccessor, times(2)).connectAndLogIn();
    verify(fafServerAccessor).disconnect();
    verify(eventBus).post(any(LoggedOutEvent.class));
    verify(eventBus, times(2)).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWithRefresh() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.loginWithRefreshToken("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithRefreshToken("abc");
    verify(fafApiAccessor).authorize();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraTokenError() {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(tokenService).loginWithRefreshToken(anyString());

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.loginWithRefreshToken("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithRefreshToken("abc");
    verify(fafApiAccessor, never()).authorize();
    verify(fafServerAccessor, never()).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testPreferencesSavedOnClose() {
    when(tokenService.getRefreshToken()).thenReturn("abe");

    instance.destroy();

    verify(tokenService).getRefreshToken();
    verify(preferencesService).storeInBackground();
    assertEquals("abe", preferencesService.getPreferences().getLogin().getRefreshToken());
  }

  @Test
  public void testOwnUserStartsAsNull() {
    assertNull(instance.getOwnUser());
  }

  @Test
  public void testOnLogoutRequest() {
    instance.onLogoutRequestEvent(new LogOutRequestEvent());

    LoginPrefs loginPrefs = preferences.getLogin();
    assertNull(loginPrefs.getRefreshToken());
    verify(preferencesService).storeInBackground();
    verify(fafServerAccessor).disconnect();
    assertNull(instance.getOwnUser());
    verify(eventBus).post(any(LoggedOutEvent.class));
  }

  @Test
  public void testOnSessionExpired() {
    instance.onSessionExpired(new SessionExpiredEvent());

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
