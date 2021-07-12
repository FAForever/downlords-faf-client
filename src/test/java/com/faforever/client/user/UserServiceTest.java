package com.faforever.client.user;

import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.MeResult;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  private FafService fafService;
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
  private LoginMessage validLoginMessage;
  private MeResult meResult;

  @BeforeEach
  public void setUp() throws Exception {
    validLoginMessage = new LoginMessage();
    PlayerInfo me = new PlayerInfo(1, "junit", null, null, null, null, null, null);
    validLoginMessage.setMe(me);
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

    instance = new UserService(clientProperties, fafService, preferencesService, eventBus, tokenService, notificationService, i18n);
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
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService).connectToServer(tokenService.getRefreshedTokenValue());
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testReLoginWhenConnected() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.CONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService, never()).connectToServer(tokenService.getRefreshedTokenValue());
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraCodeError() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(tokenService).loginWithAuthorizationCode(anyString());

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService, never()).authorizeApi();
    verify(fafService, never()).connectToServer(anyString());
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiAuthorizeError() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(fafService).authorizeApi();

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService, never()).connectToServer(anyString());
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiVerifyError() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.failedFuture(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService, never()).connectToServer(anyString());
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginLobbyError() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.failedFuture(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService).connectToServer(anyString());
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWrongUserFromServer() {
    LoginMessage invalidLoginMessage = new LoginMessage();
    PlayerInfo notMe = new PlayerInfo(100, "notMe", null, null, null, null, null, null);
    invalidLoginMessage.setMe(notMe);
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(invalidLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.failedFuture(testException));

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.login("abc").join());

    assertEquals(testException, thrown.getCause());
    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc");
    verify(fafService).authorizeApi();
    verify(fafService).connectToServer(anyString());
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginAfterExpiredWithDifferentUser() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.login("abc").join();

    MeResult otherResult = new MeResult();
    otherResult.setUserName("junit2");
    otherResult.setUserId("2");
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(otherResult));
    LoginMessage newLoginMessage = new LoginMessage();
    PlayerInfo me = new PlayerInfo(2, "junit2", null, null, null, null, null, null);
    newLoginMessage.setMe(me);
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(newLoginMessage));

    instance.login("abc").join();

    verify(fafService, times(2)).connectToServer(anyString());
    verify(fafService).disconnect();
    verify(eventBus).post(any(LoggedOutEvent.class));
    verify(eventBus, times(2)).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWithRefresh() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");

    instance.loginWithRefreshToken("abc").join();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithRefreshToken("abc");
    verify(fafService).authorizeApi();
    verify(fafService).connectToServer(tokenService.getRefreshedTokenValue());
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraTokenError() {
    when(fafService.getLobbyConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafService.getCurrentUser()).thenReturn(CompletableFuture.completedFuture(meResult));
    when(fafService.connectToServer(anyString())).thenReturn(CompletableFuture.completedFuture(validLoginMessage));
    when(tokenService.getRefreshedTokenValue()).thenReturn("def");
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(tokenService).loginWithRefreshToken(anyString());

    CompletionException thrown = assertThrows(CompletionException.class, () -> instance.loginWithRefreshToken("abc").join());

    assertEquals(testException, thrown.getCause());
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithRefreshToken("abc");
    verify(fafService, never()).authorizeApi();
    verify(fafService, never()).connectToServer(anyString());
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
    assertFalse(loginPrefs.isRememberMe());
    assertNull(loginPrefs.getRefreshToken());
    verify(preferencesService).storeInBackground();
    verify(fafService).disconnect();
    assertNull(instance.getOwnUser());
    verify(eventBus).post(any(LoggedOutEvent.class));
  }

  @Test
  public void testOnSessionExpired() {
    instance.onSessionExpired(new SessionExpiredEvent());

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
