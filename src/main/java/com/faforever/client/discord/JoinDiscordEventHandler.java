package com.faforever.client.discord;

import com.faforever.client.fx.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;

@Component
@RequiredArgsConstructor
@Slf4j
public class JoinDiscordEventHandler {

  private final PlatformService platformService;

  public void onJoin(String channelUrl) {
    joinViaDiscord(channelUrl);
  }

  private void joinViaBrowser(String joinUrl) {
    platformService.showDocument(joinUrl);
  }

  private void joinViaDiscord(String joinUrl) {
    StandardWebSocketClient client = new StandardWebSocketClient();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Origin", "https://discord.com");
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders(headers);
    client.execute(getWebSocketHandler(joinUrl), webSocketHttpHeaders, URI.create("ws://127.0.0.1:6463/?v=1"))
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.warn("Connection to Discord not possible", ex);
              joinViaBrowser(joinUrl);
            }
          });
  }

  @NotNull
  private WebSocketHandler getWebSocketHandler(String joinUrl) {
    return new WebSocketHandler() {
      @Override
      public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        log.debug("Connection to Discord established");
      }

      @Override
      public void handleMessage(@NotNull WebSocketSession session,
                                @NotNull WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage textMessage)) {
          session.close(CloseStatus.BAD_DATA);
          return;
        }

        if (textMessage.getPayload().contains("DISPATCH")) {
          session.sendMessage(new TextMessage(
              "{\"cmd\":\"INVITE_BROWSER\",\"args\":{\"code\":\"" + joinUrl.replaceAll("https://.*/",
                                                                                       "") + "\"},\"nonce\":\"bcf3dcce-e76e-44d3-8bde-d3c7e435d165\"}"));
        } else if (textMessage.getPayload().contains("INVITE_BROWSER")) {
          session.close(CloseStatus.NORMAL);
        } else {
          session.close(CloseStatus.BAD_DATA);
        }
      }

      @Override
      public void handleTransportError(@NotNull WebSocketSession session,
                                       @NotNull Throwable exception) throws Exception {
        log.warn("Unable to contact Discord app", exception);
        joinViaBrowser(joinUrl);
      }

      @Override
      public void afterConnectionClosed(@NotNull WebSocketSession session,
                                        @NotNull CloseStatus closeStatus) throws Exception {
        if (!closeStatus.equals(CloseStatus.NORMAL)) {
          log.warn("Unable to contact Discord app: {}", closeStatus);
          joinViaBrowser(joinUrl);
          return;
        }
        log.debug("Connection to Discord closed {}", closeStatus);
      }

      @Override
      public boolean supportsPartialMessages() {
        return false;
      }
    };
  }
}
