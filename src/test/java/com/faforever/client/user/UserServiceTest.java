package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest extends ServiceTest {

  private static final String BASE_URL = "https://example.com";
  private static final String CLIENT_ID = "test";
  private static final URI REDIRECT_URI = URI.create("http://localhost");
  private static final String SCOPES = "scope";
  public static final String STATE = "abc";
  public static final String VERIFIER = "def";

  @Spy
  private ClientProperties clientProperties;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private FafApiAccessor fafApiAccessor;

  @Mock
  private EventBus eventBus;
  @Mock
  private TokenService tokenService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Spy
  private LoginPrefs loginPrefs;

  @InjectMocks
  private UserService instance;
  private MeResult meResult;
  private Player me;

  @BeforeEach
  public void setUp() throws Exception {
    me = new Player(1, "junit", null, null, "", new HashMap<>(), new HashMap<>());
    meResult = new MeResult();
    meResult.setUserName("junit");
    meResult.setUserId("1");

    Oauth oauth = clientProperties.getOauth();
    oauth.setBaseUrl(BASE_URL);
    oauth.setClientId(CLIENT_ID);
    oauth.setRedirectUri(REDIRECT_URI);
    oauth.setScopes(SCOPES);

    instance.afterPropertiesSet();
    
    verify(eventBus).register(instance);
  }

  @Test
  public void testGetHydraUrl() {
    String url = instance.getHydraUrl(STATE, VERIFIER, REDIRECT_URI);
    assertTrue(url.contains(BASE_URL));
    assertTrue(url.contains(CLIENT_ID));
    assertTrue(url.contains(REDIRECT_URI.toASCIIString()));
    assertTrue(url.contains(SCOPES));
  }

  @Test
  public void testLogin() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.loginWithRefreshToken()).thenReturn(Mono.empty());
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testReLoginWhenConnected() throws Exception {
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService, times(2)).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor, times(2)).getMe();
    verify(fafServerAccessor, times(1)).connectAndLogIn();
    verify(eventBus, times(2)).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraCodeError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    FakeTestException testException = new FakeTestException("failed");
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiAuthorizeError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginApiVerifyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginLobbyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWrongUserFromServer() throws Exception {
    Player notMe = new Player(100, "notMe", null, null, "", new HashMap<>(), new HashMap<>());
    LoginSuccessResponse invalidLoginMessage = new LoginSuccessResponse(notMe);
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(notMe));
    when(tokenService.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    verify(tokenService).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginWithRefresh() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    when(tokenService.loginWithRefreshToken()).thenReturn(Mono.empty());

    instance.loginWithRefreshToken().block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    verify(tokenService).loginWithRefreshToken();
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(eventBus).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testLoginHydraTokenError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenService.getRefreshedTokenValue()).thenReturn(Mono.just("def"));
    FakeTestException testException = new FakeTestException("failed");
    when(tokenService.loginWithRefreshToken()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.loginWithRefreshToken().block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    verify(tokenService).loginWithRefreshToken();
    verify(eventBus, never()).post(any(LoginSuccessEvent.class));
  }

  @Test
  public void testOwnUserStartsAsNull() {
    assertNull(instance.getOwnUser());
  }

  @Test
  public void testOnLogoutRequest() {
    instance.onLogoutRequestEvent(new LogOutRequestEvent());

    assertNull(loginPrefs.getRefreshToken());
    verify(fafServerAccessor).disconnect();
    assertNull(instance.getOwnUser());
    verify(eventBus).post(any(LoggedOutEvent.class));
  }

  @Test
  public void testOnSessionExpired() {
    instance.onSessionExpired(new SessionExpiredEvent());

    verify(notificationService).addImmediateInfoNotification(anyString());
  }
}
