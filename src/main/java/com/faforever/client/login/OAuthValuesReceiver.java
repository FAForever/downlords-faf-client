package com.faforever.client.login;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.user.LoginService;
import lombok.RequiredArgsConstructor;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

  private final PlatformService platformService;
  private final LoginService loginService;
  private final I18n i18n;

  private CountDownLatch redirectUriLatch;
  private URI redirectUri;
  private String state;
  private String codeVerifier;
  private CompletableFuture<Values> valuesFuture;

  public CompletableFuture<Values> receiveValues(String state, String codeVerifier) {
    if (valuesFuture != null && !valuesFuture.isDone()) {
      return valuesFuture;
    }

    redirectUriLatch = new CountDownLatch(1);
    valuesFuture = CompletableFuture.supplyAsync(() -> readValues(state, codeVerifier));
    return valuesFuture;

  }

  public CompletableFuture<Void> openBrowserToLogin() {
    if (redirectUriLatch == null) {
      throw new IllegalStateException("Redirect socket is not open");
    }

    return CompletableFuture.runAsync(() -> {
      try {
        redirectUriLatch.await();
      } catch (InterruptedException ignored) {}
      platformService.showDocument(loginService.getHydraUrl(this.state, this.codeVerifier, redirectUri));
    });
  }

  private Values readValues(String state, String codeVerifier) {
    this.state = state;
    this.codeVerifier = codeVerifier;
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      redirectUri = UriComponentsBuilder.fromUriString("http://127.0.0.1")
                                        .port(serverSocket.getLocalPort())
                                        .build()
                                        .toUri();
      redirectUriLatch.countDown();

      platformService.showDocument(loginService.getHydraUrl(this.state, this.codeVerifier, redirectUri));

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
    } catch (IOException e) {
      throw new IllegalStateException("Could not get code", e);
    } finally {
      redirectUriLatch = null;
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

  public record Values(String code, String state, URI redirectUri) {}
}
