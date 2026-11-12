package com.faforever.client.discord.ipc;

import com.faforever.client.discord.ipc.DiscordActivity.Party;
import com.faforever.client.discord.ipc.DiscordActivity.Secrets;
import com.faforever.client.discord.ipc.DiscordActivity.Timestamps;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiscordIpcClientTest extends ServiceTest {

  private static final String APPLICATION_ID = "1234";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @TempDir
  Path tempDir;
  @Mock
  private DiscordIpcListener listener;

  private ServerSocketChannel server;
  private SocketChannel discord;
  private DiscordIpcClient instance;

  @BeforeEach
  public void setUp() throws Exception {
    Path socketPath = tempDir.resolve("discord-ipc-0");
    server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
    server.bind(UnixDomainSocketAddress.of(socketPath));
    instance = DiscordIpcClient.start(APPLICATION_ID, listener, () -> DiscordIpcPipe.UnixPipe.connect(socketPath),
                                      Duration.ofMillis(50));
    discord = server.accept();
  }

  @AfterEach
  public void tearDown() throws Exception {
    instance.close();
    discord.close();
    server.close();
  }

  @Test
  public void handshakeAndSubscribe() throws Exception {
    Frame handshake = readFrame();
    assertThat(handshake.opcode(), is(0));
    assertThat(handshake.payload().get("v").asInt(), is(1));
    assertThat(handshake.payload().get("client_id").asText(), is(APPLICATION_ID));

    sendReady();

    verify(listener, timeout(1000)).onReady(new DiscordUser("42", "someone"));
    for (String event : List.of("ACTIVITY_JOIN", "ACTIVITY_SPECTATE", "ACTIVITY_JOIN_REQUEST")) {
      Frame subscribe = readFrame();
      assertThat(subscribe.opcode(), is(1));
      assertThat(subscribe.payload().get("cmd").asText(), is("SUBSCRIBE"));
      assertThat(subscribe.payload().get("evt").asText(), is(event));
    }
  }

  @Test
  public void activitySetBeforeReadyIsSentAfterReady() throws Exception {
    instance.setActivity(new DiscordActivity("Hosting", "faf | Game", new Timestamps(1700000000000L), null,
                                             new Party("5", List.of(1, 8)), new Secrets("{\"gameId\":5}", null)));
    connect();

    JsonNode setActivity = readFrame().payload();
    assertThat(setActivity.get("cmd").asText(), is("SET_ACTIVITY"));
    assertThat(setActivity.get("args").get("pid").asLong(), is(ProcessHandle.current().pid()));
    assertThat(setActivity.get("args").get("activity"), is(objectMapper.readTree("""
        {
          "state": "Hosting",
          "details": "faf | Game",
          "timestamps": {"start": 1700000000000},
          "party": {"id": "5", "size": [1, 8]},
          "secrets": {"join": "{\\"gameId\\":5}"}
        }
        """)));
  }

  @Test
  public void clearActivity() throws Exception {
    connect();

    instance.setActivity(null);

    JsonNode setActivity = readFrame().payload();
    assertThat(setActivity.get("cmd").asText(), is("SET_ACTIVITY"));
    assertThat(setActivity.get("args").get("activity").isNull(), is(true));
  }

  @Test
  public void joinAndSpectateAreDispatched() throws Exception {
    connect();

    sendFrame(1, """
        {"cmd":"DISPATCH","evt":"ACTIVITY_JOIN","data":{"secret":"join-secret"}}""");
    sendFrame(1, """
        {"cmd":"DISPATCH","evt":"ACTIVITY_SPECTATE","data":{"secret":"spectate-secret"}}""");

    verify(listener, timeout(1000)).onJoin("join-secret");
    verify(listener, timeout(1000)).onSpectate("spectate-secret");
  }

  @Test
  public void joinRequestIsAnswered() throws Exception {
    when(listener.onJoinRequest(new DiscordUser("7", "friend"))).thenReturn(true, false);
    connect();

    sendFrame(1, """
        {"cmd":"DISPATCH","evt":"ACTIVITY_JOIN_REQUEST","data":{"user":{"id":"7","username":"friend"}}}""");
    JsonNode accept = readFrame().payload();
    assertThat(accept.get("cmd").asText(), is("SEND_ACTIVITY_JOIN_INVITE"));
    assertThat(accept.get("args").get("user_id").asText(), is("7"));

    sendFrame(1, """
        {"cmd":"DISPATCH","evt":"ACTIVITY_JOIN_REQUEST","data":{"user":{"id":"7","username":"friend"}}}""");
    JsonNode reject = readFrame().payload();
    assertThat(reject.get("cmd").asText(), is("CLOSE_ACTIVITY_REQUEST"));
  }

  @Test
  public void pingIsAnsweredWithPong() throws Exception {
    connect();

    sendFrame(3, "{\"nonce\":\"abc\"}");

    Frame pong = readFrame();
    assertThat(pong.opcode(), is(4));
    assertThat(pong.payload().get("nonce").asText(), is("abc"));
  }

  @Test
  public void reconnectsAndResendsActivityAfterConnectionLoss() throws Exception {
    connect();
    instance.setActivity(new DiscordActivity("Playing", null, null, null, null, null));
    readFrame();

    discord.close();
    discord = server.accept();
    connect();

    JsonNode setActivity = readFrame().payload();
    assertThat(setActivity.get("args").get("activity").get("state").asText(), is("Playing"));
  }

  /** Completes the handshake and consumes the subscriptions. */
  private void connect() throws IOException {
    readFrame();
    sendReady();
    for (int i = 0; i < 3; i++) {
      readFrame();
    }
  }

  private void sendReady() throws IOException {
    sendFrame(1, """
        {"cmd":"DISPATCH","evt":"READY","data":{"v":1,"user":{"id":"42","username":"someone","avatar":null}}}""");
  }

  private void sendFrame(int opcode, String json) throws IOException {
    byte[] payload = json.getBytes(StandardCharsets.UTF_8);
    ByteBuffer frame = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN)
                                 .putInt(opcode).putInt(payload.length).put(payload).flip();
    while (frame.hasRemaining()) {
      discord.write(frame);
    }
  }

  private Frame readFrame() throws IOException {
    ByteBuffer header = readFully(8).order(ByteOrder.LITTLE_ENDIAN);
    int opcode = header.getInt();
    int length = header.getInt();
    return new Frame(opcode, objectMapper.readTree(readFully(length).array()));
  }

  private ByteBuffer readFully(int length) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(length);
    while (buffer.hasRemaining()) {
      if (discord.read(buffer) < 0) {
        throw new IOException("Connection closed");
      }
    }
    return buffer.flip();
  }

  private record Frame(int opcode, JsonNode payload) {}
}
