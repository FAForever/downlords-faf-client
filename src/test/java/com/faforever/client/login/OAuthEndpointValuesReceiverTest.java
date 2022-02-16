package com.faforever.client.login;

import com.faforever.client.builders.ClientConfigurationBuilder.OAuthEndpointBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.ClientConfiguration.OAuthEndpoint;
import com.faforever.client.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthEndpointValuesReceiverTest extends ServiceTest {

  private static final String TITLE = "JUnit Login Success";
  private static final String MESSAGE = "JUnit Login Message";
  public static final URI REDIRECT_URI = URI.create("http://localhost");

  @InjectMocks
  private OAuthValuesReceiver instance;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private UserService userService;
  @Spy
  private ClientProperties clientProperties = new ClientProperties();

  @BeforeEach
  void setUp() {
    when(i18n.get("login.browser.success.title")).thenReturn(TITLE);
    when(i18n.get("login.browser.success.message")).thenReturn(MESSAGE);
    clientProperties.getOauth().setTimeout(1000);
  }

  @Test
  void receiveValues() throws Exception {
    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(1000)).getHydraUrl(captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(TITLE));
      assertThat(response, containsString(MESSAGE));
    }

    Values values = future.get();
    assertThat(values.getCode(), is("1234"));
    assertThat(values.getState(), is("abcd"));
  }

  @Test
  void receiveValuesTimeout() throws Exception {
    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof SocketTimeoutException);
  }

  @Test
  void receiveValuesNoPortsAvailable() throws Exception {
    ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    URI takenPort = URI.create("http://localhost:" + serverSocket.getLocalPort());
    CompletableFuture<Values> future = instance.receiveValues(Optional.of(takenPort), Optional.empty());
    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }
}