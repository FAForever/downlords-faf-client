package com.faforever.client.login;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Opens a minimal HTTP server that retrieves {@literal code} and {@literal state} from the browser. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthValuesReceiver {

  private static final Pattern CODE_PATTERN = Pattern.compile("code=([^ &]+)");
  private static final Pattern STATE_PATTERN = Pattern.compile("state=([^ &]+)");
  private static final List<String> ALLOWED_HOSTS = List.of("localhost", "127.0.0.1");

  private final PlatformService platformService;
  private final UserService userService;
  private final I18n i18n;

  private CountDownLatch redirectUriLatch;
  private CompletableFuture<Values> valuesFuture;
  private URI redirectUri;
  private String state;
  private String codeVerifier;

  public CompletableFuture<Values> receiveValues(List<URI> redirectUriCandidates, String state, String codeVerifier) {
    this.state = state;
    this.codeVerifier = codeVerifier;
    if (valuesFuture == null || valuesFuture.isDone()) {
      if (redirectUriCandidates == null || redirectUriCandidates.isEmpty()) {
        throw new IllegalArgumentException("No redirect uris provided");
      }

      redirectUriLatch = new CountDownLatch(1);
      valuesFuture = CompletableFuture.supplyAsync(() -> {
        List<URI> filteredRedirectUris = redirectUriCandidates.stream()
            .filter(uri -> ALLOWED_HOSTS.contains(uri.getHost()))
            .toList();
        for (URI uri : filteredRedirectUris) {
          try {
            return readWithUri(uri);
          } catch (SocketException e) {
            log.info("Port `{}` is probably already in use", uri.getPort(), e);
          } catch (IOException e) {
            throw new IllegalStateException("Could not read from port once opened", e);
          }
        }
        throw new IllegalStateException("Could not read from any redirect URI: " + redirectUriCandidates);
      });
    } else {
      CompletableFuture.runAsync(() -> {
        try {
          redirectUriLatch.await();
        } catch (InterruptedException ignored) {}
        platformService.showDocument(userService.getHydraUrl(this.state, this.codeVerifier, this.redirectUri));
      });
    }

    return valuesFuture;
  }

  private Values readWithUri(URI uri) throws IOException {
    // Usually, a random port can't be used since the redirect URI, including port, must be registered on the server
    try (ServerSocket serverSocket = new ServerSocket(Math.max(0, uri.getPort()), 1, InetAddress.getLoopbackAddress())) {
      redirectUri = UriComponentsBuilder.fromUri(uri).port(serverSocket.getLocalPort()).build().toUri();

      platformService.showDocument(userService.getHydraUrl(this.state, this.codeVerifier, redirectUri));
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
