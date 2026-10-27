package com.faforever.client.discord;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.test.ServiceTest;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class JoinDiscordEventHandlerTest extends ServiceTest {

  private static final String JOIN_URL = "https://discord.gg/abc123";

  @Mock
  private PlatformService platformService;

  @InjectMocks
  private JoinDiscordEventHandler instance;

  private MockWebServer server;

  @BeforeEach
  public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    instance.discordRpcUri = URI.create("ws://127.0.0.1:" + server.getPort() + "/?v=1");
  }

  @AfterEach
  public void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testInviteSentAndNoBrowserFallback() throws Exception {
    CompletableFuture<String> receivedCommand = new CompletableFuture<>();
    CompletableFuture<Integer> closeCode = new CompletableFuture<>();
    server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
      @Override
      public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send("{\"cmd\":\"DISPATCH\",\"evt\":\"READY\"}");
      }

      @Override
      public void onMessage(WebSocket webSocket, String text) {
        receivedCommand.complete(text);
        webSocket.send("{\"cmd\":\"INVITE_BROWSER\",\"data\":{}}");
      }

      @Override
      public void onClosing(WebSocket webSocket, int code, String reason) {
        closeCode.complete(code);
        webSocket.close(code, null);
      }
    }));

    instance.onJoin(JOIN_URL);

    String command = receivedCommand.get(5, TimeUnit.SECONDS);
    assertTrue(command.contains("\"cmd\":\"INVITE_BROWSER\""), command);
    assertTrue(command.contains("\"code\":\"abc123\""), command);
    assertEquals(1000, closeCode.get(5, TimeUnit.SECONDS));
    verify(platformService, after(500).never()).showDocument(JOIN_URL);
  }

  @Test
  public void testUnexpectedMessageFallsBackToBrowserOnce() {
    server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
      @Override
      public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send("garbage");
      }

      @Override
      public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(code, null);
      }
    }));

    instance.onJoin(JOIN_URL);

    verify(platformService, timeout(5000)).showDocument(JOIN_URL);
    verify(platformService, after(500).times(1)).showDocument(JOIN_URL);
  }

  @Test
  public void testDiscordNotRunningFallsBackToBrowser() throws IOException {
    int freePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      freePort = socket.getLocalPort();
    }
    instance.discordRpcUri = URI.create("ws://127.0.0.1:" + freePort + "/?v=1");

    instance.onJoin(JOIN_URL);

    verify(platformService, timeout(5000)).showDocument(JOIN_URL);
  }
}
