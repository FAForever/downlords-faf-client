package com.faforever.client.login;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.update.ClientConfiguration.OAuthEndpoint;
import com.faforever.client.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Opens a minimal HTTP server that retrieves {@literal code} and {@literal state} from the browser. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthValuesReceiver {

  private static final Pattern CODE_PATTERN = Pattern.compile("code=([^ &]+)");
  private static final Pattern STATE_PATTERN = Pattern.compile("state=([^ &]+)");
  private static final List<String> ALLOWED_HOSTS = List.of("localhost");

  private final ClientProperties clientProperties;
  private final PlatformService platformService;
  private final UserService userService;
  private final I18n i18n;

  private CountDownLatch redirectUriLatch;
  private CompletableFuture<Values> valuesFuture;
  private URI redirectUri;

  public CompletableFuture<Values> receiveValues(Optional<URI> redirectUri, Optional<OAuthEndpoint> oAuthEndpoint) {
    if (valuesFuture == null || valuesFuture.isDone()) {
      redirectUriLatch = new CountDownLatch(1);
      valuesFuture = CompletableFuture.supplyAsync(() -> {
        Collection<URI> redirectUris = getRedirectUris(redirectUri, oAuthEndpoint);

        for (URI uri : redirectUris) {
          try {
            return readWithUri(uri);
          } catch (SocketException e) {
            log.info("Port `{}` is probably already in use", uri.getPort(), e);
          } catch (IOException e) {
            throw new IllegalStateException("Could not read from port once opened", e);
          }
        }
        throw new IllegalStateException("Could not read from any redirect URI: " + redirectUris);
      });
    } else {
      CompletableFuture.runAsync(() -> {
        try {
          redirectUriLatch.await();
        } catch (InterruptedException ignored) {}
        platformService.showDocument(userService.getHydraUrl(this.redirectUri));
      });
    }

    return valuesFuture;
  }

  private List<URI> getRedirectUris(Optional<URI> redirectUri, Optional<OAuthEndpoint> oAuthEndpoint) {
    JavaFxUtil.assertBackgroundThread();
    if (redirectUri.isPresent()) {
      return Collections.singletonList(redirectUri.get());
    }

    if (!clientProperties.isUseRemotePreferences()) {
      throw new NoRedirectUriException("No redirect URI has been specified and remote preferences are disabled");
    }

    OAuthEndpoint endpoint = oAuthEndpoint.orElseThrow(() -> new IllegalStateException("No endpoint has been specified"));
    List<URI> redirectUris = endpoint.getRedirectUris().stream()
        .filter(uri -> uri.getPort() > 0)
        .filter(uri -> ALLOWED_HOSTS.contains(uri.getHost()))
        .collect(Collectors.toList());

    if (redirectUris.isEmpty()) {
      throw new NoRedirectUriException("No valid redirect URI has been provided by endpoint: " + endpoint.getUrl());
    }

    return redirectUris;
  }

  private Values readWithUri(URI uri) throws IOException {
    // Usually, a random port can't be used since the redirect URI, including port, must be registered on the server
    try (ServerSocket serverSocket = new ServerSocket(Math.max(0, uri.getPort()), 1, InetAddress.getLoopbackAddress())) {
      redirectUri = UriComponentsBuilder.fromUri(uri).port(serverSocket.getLocalPort()).build().toUri();

      platformService.showDocument(userService.getHydraUrl(redirectUri));
      redirectUriLatch.countDown();

      Socket socket = serverSocket.accept();
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      String request = reader.readLine();
      log.info(request);

      boolean success = false;

      // Do not try with resources as the socket needs to stay open.
      try {
        Values values = readValues(request, redirectUri);
        success = true;
        return values;
      } finally {
        writeResponse(socket, success);
        reader.close();
      }
    }
  }

  private void writeResponse(Socket socket, boolean success) throws IOException {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

      String html;

      if (success) {
        html = new String(OAuthValuesReceiver.class.getResourceAsStream("/login_success.html").readAllBytes())
            .replace("${title}", i18n.get("login.browser.success.title"))
            .replace("${message}", i18n.get("login.browser.success.message"));
      } else {
        html = new String(OAuthValuesReceiver.class.getResourceAsStream("/login_failed.html").readAllBytes())
            .replace("${title}", i18n.get("login.browser.failed.title"))
            .replace("${message}", i18n.get("login.browser.failed.message"));
      }

      writer
          .append("HTTP/1.1 200 OK\r\n")
          .append("Content-Length ")
          .append(String.valueOf(html.length()))
          .append("\r\n")
          .append("Content-Type: text/html\r\n")
          .append("Connection: Closed\r\n")
          .append("\r\n")
          .append(html);
    }
  }

  private Values readValues(String request, URI redirectUri) {
    String code = extractValue(request, CODE_PATTERN);
    String state = extractValue(request, STATE_PATTERN);
    return new Values(code, state, redirectUri);
  }

  private String extractValue(String request, Pattern pattern) {
    Matcher matcher = pattern.matcher(request);
    if (!matcher.find()) {
      throw new IllegalStateException("Could not extract value with pattern '" + pattern + "' from: " + request);
    }
    return matcher.group(1);
  }

  @Value
  public static class Values {
    String code;
    String state;
    URI redirectUri;
  }
}
