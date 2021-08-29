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
import org.mockito.Mock;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

class OAuthEndpointValuesReceiverTest extends ServiceTest {

  private static final String TITLE = "JUnit Login Success";
  private static final String MESSAGE = "JUnit Login Message";
  public static final URI REDIRECT_URI = URI.create("http://localhost");

  private OAuthValuesReceiver instance;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private UserService userService;

  @BeforeEach
  void setUp() {
    when(i18n.get("login.browser.success.title")).thenReturn(TITLE);
    when(i18n.get("login.browser.success.message")).thenReturn(MESSAGE);
    ClientProperties clientProperties = new ClientProperties();
    instance = new OAuthValuesReceiver(clientProperties, platformService, userService, i18n);
  }

  @Test
  void receiveValues() throws Exception {
    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    Integer port = instance.listenPort.get();
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(URI.create("http://localhost:" + port))
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
}