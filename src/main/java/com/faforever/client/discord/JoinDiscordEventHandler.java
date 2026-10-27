package com.faforever.client.discord;

import com.faforever.client.fx.PlatformService;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
@Slf4j
public class JoinDiscordEventHandler {

  private static final URI DISCORD_RPC_URI = URI.create("ws://127.0.0.1:6463/?v=1");
  // Invalid frame payload data, equivalent to Spring's CloseStatus.BAD_DATA
  private static final int BAD_DATA = 1007;

  private final PlatformService platformService;

  @VisibleForTesting
  URI discordRpcUri = DISCORD_RPC_URI;

  public void onJoin(String channelUrl) {
    joinViaDiscord(channelUrl);
  }

  private void joinViaBrowser(String joinUrl) {
    platformService.showDocument(joinUrl);
  }

  private void joinViaDiscord(String joinUrl) {
    HttpClient httpClient = HttpClient.newHttpClient();
    httpClient.newWebSocketBuilder()
              .header("Origin", "https://discord.com")
              .buildAsync(discordRpcUri, new DiscordInviteListener(httpClient, joinUrl))
              .whenComplete((result, ex) -> {
                if (ex != null) {
                  log.warn("Connection to Discord not possible", ex);
                  httpClient.shutdownNow();
                  joinViaBrowser(joinUrl);
                }
              });
  }

  @RequiredArgsConstructor
  private class DiscordInviteListener implements WebSocket.Listener {
    private final HttpClient httpClient;
    private final String joinUrl;
    private final StringBuilder messageBuffer = new StringBuilder();
    private boolean finished;

    @Override
    public void onOpen(WebSocket webSocket) {
      log.debug("Connection to Discord established");
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      messageBuffer.append(data);
      if (!last) {
        webSocket.request(1);
        return null;
      }

      String payload = messageBuffer.toString();
      messageBuffer.setLength(0);

      if (payload.contains("DISPATCH")) {
        webSocket.sendText(
            "{\"cmd\":\"INVITE_BROWSER\",\"args\":{\"code\":\"" + joinUrl.replaceAll("https://.*/",
                                                                                     "") + "\"},\"nonce\":\"bcf3dcce-e76e-44d3-8bde-d3c7e435d165\"}",
            true);
        webSocket.request(1);
      } else if (payload.contains("INVITE_BROWSER")) {
        log.debug("Connection to Discord closed");
        finish(webSocket, WebSocket.NORMAL_CLOSURE, false);
      } else {
        log.warn("Unexpected message from Discord: {}", payload);
        finish(webSocket, BAD_DATA, true);
      }
      return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
      log.warn("Unexpected binary message from Discord");
      finish(webSocket, BAD_DATA, true);
      return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      if (statusCode != WebSocket.NORMAL_CLOSURE) {
        log.warn("Unable to contact Discord app: {} {}", statusCode, reason);
      }
      finish(webSocket, WebSocket.NORMAL_CLOSURE, statusCode != WebSocket.NORMAL_CLOSURE);
      return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      log.warn("Unable to contact Discord app", error);
      finish(webSocket, BAD_DATA, true);
    }

    /**
     * Closes the connection and releases the client. Falls back to the browser at most once.
     */
    private synchronized void finish(WebSocket webSocket, int statusCode, boolean fallbackToBrowser) {
      if (finished) {
        return;
      }
      finished = true;

      webSocket.sendClose(statusCode, "").whenComplete((ignored, ex) -> {
        webSocket.abort();
        httpClient.shutdownNow();
      });
      if (fallbackToBrowser) {
        joinViaBrowser(joinUrl);
      }
    }
  }
}
