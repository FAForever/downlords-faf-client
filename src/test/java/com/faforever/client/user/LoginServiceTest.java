package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.TokenRetriever;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

import java.net.URI;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginServiceTest extends ServiceTest {

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
  private TokenRetriever tokenRetriever;
  @Mock
  private NotificationService notificationService;
  @Spy
  private LoginPrefs loginPrefs;

  @InjectMocks
  private LoginService instance;
  private MeResult meResult;
  private Player me;

  private final TestPublisher<Long> invalidationTestPublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    me = new Player(1, "junit", null, null, "", new HashMap<>(), new HashMap<>(), null);
    meResult = new MeResult();
    meResult.setUserName("junit");
    meResult.setUserId("1");

    Oauth oauth = clientProperties.getOauth();
    oauth.setBaseUrl(BASE_URL);
    oauth.setClientId(CLIENT_ID);
    oauth.setScopes(SCOPES);

    when(tokenRetriever.invalidationFlux()).thenReturn(invalidationTestPublisher.flux());

    instance.afterPropertiesSet();
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
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    assertTrue(instance.isLoggedIn());
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testReLoginWhenConnected() throws Exception {
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

    instance.login("abc", VERIFIER, REDIRECT_URI).block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    assertTrue(instance.isLoggedIn());
    verify(tokenRetriever, times(2)).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor, times(2)).getMe();
    verify(fafServerAccessor, times(1)).connectAndLogIn();
  }

  @Test
  public void testLoginHydraCodeError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    FakeTestException testException = new FakeTestException("failed");
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
  }

  @Test
  public void testLoginApiAuthorizeError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
  }

  @Test
  public void testLoginApiVerifyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
  }

  @Test
  public void testLoginLobbyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testLoginWrongUserFromServer() throws Exception {
    Player notMe = new Player(100, "notMe", null, null, "", new HashMap<>(), new HashMap<>(), null);
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(notMe));
    when(tokenRetriever.loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.login("abc", VERIFIER, REDIRECT_URI)
        .block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithAuthorizationCode("abc", VERIFIER, REDIRECT_URI);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testLoginWithRefresh() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithRefreshToken()).thenReturn(Mono.empty());

    instance.loginWithRefreshToken().block();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    assertTrue(instance.isLoggedIn());
    verify(tokenRetriever).loginWithRefreshToken();
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testLoginHydraTokenError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    FakeTestException testException = new FakeTestException("failed");
    when(tokenRetriever.loginWithRefreshToken()).thenReturn(Mono.error(testException));

    FakeTestException thrown = assertThrows(FakeTestException.class, () -> instance.loginWithRefreshToken().block());

    assertEquals(testException, thrown);
    assertNull(instance.getOwnUser());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithRefreshToken();
  }

  @Test
  public void testOwnUserStartsAsNull() {
    assertNull(instance.getOwnUser());
  }

  @Test
  public void testOnSessionExpired() throws Exception {
    testLogin();
    invalidationTestPublisher.next(0L);

    verify(notificationService).addImmediateInfoNotification(anyString());
  }
}
