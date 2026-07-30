package com.faforever.client.user;

import com.faforever.client.api.DeviceCodeResponse;
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
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginServiceTest extends ServiceTest {

  private static final String BASE_URL = "https://example.com";
  private static final String CLIENT_ID = "test";
  private static final String SCOPES = "scope";
  private static final DeviceCodeResponse DEVICE_CODE = new DeviceCodeResponse("device", "USER-CODE",
                                                                               "https://verify.faforever.com",
                                                                               "https://verify.faforever.com?user_code=USER-CODE",
                                                                               600, 5);

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
  public void testStartDeviceLogin() {
    when(tokenRetriever.initializeDeviceFlow()).thenReturn(Mono.just(DEVICE_CODE));

    StepVerifier.create(instance.startDeviceLogin()).expectNext(DEVICE_CODE).verifyComplete();

    verify(tokenRetriever).initializeDeviceFlow();
  }

  @Test
  public void testLogin() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyComplete();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    assertTrue(instance.isLoggedIn());
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testReLoginWhenConnected() throws Exception {
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyComplete();

    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyComplete();

    assertEquals(Integer.parseInt(meResult.getUserId()), (int) instance.getUserId());
    assertEquals(meResult.getUserName(), instance.getUsername());
    assertTrue(instance.isLoggedIn());
    verify(tokenRetriever, times(2)).loginWithDeviceCode(DEVICE_CODE);
    verify(fafApiAccessor, times(2)).getMe();
    verify(fafServerAccessor, times(1)).connectAndLogIn();
  }

  @Test
  public void testLoginTokenError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    FakeTestException testException = new FakeTestException("failed");
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.error(testException));

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyError();

    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
  }

  @Test
  public void testLoginApiAuthorizeError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    doThrow(testException).when(fafApiAccessor).authorize();

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyError();

    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(fafApiAccessor).authorize();
    verify(fafApiAccessor, never()).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
  }

  @Test
  public void testLoginApiVerifyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafApiAccessor.getMe()).thenReturn(Mono.error(testException));

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyError();

    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
  }

  @Test
  public void testLoginLobbyError() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyError();

    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testLoginWrongUserFromServer() throws Exception {
    Player notMe = new Player(100, "notMe", null, null, "", new HashMap<>(), new HashMap<>(), null);
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(notMe));
    when(tokenRetriever.loginWithDeviceCode(DEVICE_CODE)).thenReturn(Mono.empty());
    FakeTestException testException = new FakeTestException("failed");
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.error(testException));

    StepVerifier.create(instance.login(DEVICE_CODE)).verifyError();

    assertNull(instance.getOwnUser());
    assertNull(instance.getOwnPlayer());
    assertFalse(instance.isLoggedIn());
    verify(tokenRetriever).loginWithDeviceCode(DEVICE_CODE);
    verify(fafApiAccessor).getMe();
    verify(fafServerAccessor).connectAndLogIn();
  }

  @Test
  public void testLoginWithRefresh() throws Exception {
    when(fafServerAccessor.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(fafApiAccessor.getMe()).thenReturn(Mono.just(meResult));
    when(fafServerAccessor.connectAndLogIn()).thenReturn(Mono.just(me));
    when(tokenRetriever.loginWithRefreshToken()).thenReturn(Mono.empty());

    StepVerifier.create(instance.loginWithRefreshToken()).verifyComplete();

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

    StepVerifier.create(instance.loginWithRefreshToken()).verifyError();

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
