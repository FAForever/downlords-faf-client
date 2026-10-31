package com.faforever.client.discord.ipc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client for the local Discord RPC over IPC, see <a href="https://docs.discord.com/developers/topics/rpc">RPC</a>. It
 * keeps reconnecting while Discord isn't running and re-sends the current activity after each connect. All I/O happens
 * on a single thread.
 */
@Slf4j
public class DiscordIpcClient implements AutoCloseable {

  private static final int OP_HANDSHAKE = 0;
  private static final int OP_FRAME = 1;
  private static final int OP_CLOSE = 2;
  private static final int OP_PING = 3;
  private static final int OP_PONG = 4;
  private static final int HEADER_SIZE = 8;
  private static final int MAX_PAYLOAD_SIZE = 1024 * 1024;
  private static final long POLL_INTERVAL_MILLIS = 100;
  private static final Duration DEFAULT_RECONNECT_DELAY = Duration.ofSeconds(15);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicBoolean activityChanged = new AtomicBoolean();
  private final String applicationId;
  private final DiscordIpcListener listener;
  private final DiscordIpcPipe.Opener pipeOpener;
  private final Duration reconnectDelay;
  private final Thread thread;

  private volatile boolean running = true;
  private volatile DiscordActivity activity;
  private boolean ready;

  DiscordIpcClient(String applicationId, DiscordIpcListener listener, DiscordIpcPipe.Opener pipeOpener,
                   Duration reconnectDelay) {
    this.applicationId = applicationId;
    this.listener = listener;
    this.pipeOpener = pipeOpener;
    this.reconnectDelay = reconnectDelay;
    this.thread = Thread.ofPlatform().name("discord-ipc").daemon().unstarted(this::run);
  }

  public static DiscordIpcClient start(String applicationId, DiscordIpcListener listener) {
    return start(applicationId, listener, DiscordIpcPipe::open, DEFAULT_RECONNECT_DELAY);
  }

  static DiscordIpcClient start(String applicationId, DiscordIpcListener listener, DiscordIpcPipe.Opener pipeOpener,
                                Duration reconnectDelay) {
    DiscordIpcClient client = new DiscordIpcClient(applicationId, listener, pipeOpener, reconnectDelay);
    client.thread.start();
    return client;
  }

  /** Sets the activity shown in the user's profile, or clears it if {@code null}. */
  public void setActivity(DiscordActivity activity) {
    this.activity = activity;
    activityChanged.set(true);
  }

  @Override
  public void close() {
    running = false;
    thread.interrupt();
  }

  private void run() {
    while (running) {
      try (DiscordIpcPipe pipe = pipeOpener.open()) {
        log.debug("Connected to Discord");
        send(pipe, OP_HANDSHAKE, objectMapper.createObjectNode().put("v", 1).put("client_id", applicationId));
        handleMessages(pipe);
      } catch (IOException e) {
        log.debug("Not connected to Discord ({})", e.getMessage());
      } catch (RuntimeException e) {
        log.warn("Error in Discord IPC", e);
      } catch (InterruptedException e) {
        return;
      } finally {
        ready = false;
      }

      try {
        Thread.sleep(reconnectDelay);
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  private void handleMessages(DiscordIpcPipe pipe) throws IOException, InterruptedException {
    while (running) {
      if (ready && activityChanged.getAndSet(false)) {
        ObjectNode args = objectMapper.createObjectNode().put("pid", ProcessHandle.current().pid());
        args.set("activity", objectMapper.valueToTree(activity));
        sendCommand(pipe, "SET_ACTIVITY", args);
      }

      if (pipe.available() >= HEADER_SIZE) {
        handleFrame(pipe);
      } else {
        Thread.sleep(POLL_INTERVAL_MILLIS);
      }
    }
  }

  private void handleFrame(DiscordIpcPipe pipe) throws IOException {
    byte[] header = new byte[HEADER_SIZE];
    pipe.readFully(header);
    ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
    int opcode = headerBuffer.getInt();
    int length = headerBuffer.getInt();
    if (length < 0 || length > MAX_PAYLOAD_SIZE) {
      throw new IOException("Invalid Discord frame length: " + length);
    }
    byte[] payload = new byte[length];
    pipe.readFully(payload);
    JsonNode message = objectMapper.readTree(payload);

    switch (opcode) {
      case OP_FRAME -> handleDispatch(pipe, message);
      case OP_PING -> send(pipe, OP_PONG, message);
      case OP_CLOSE -> throw new IOException("Discord closed the connection: " + message);
      default -> log.debug("Ignoring Discord frame with opcode {}: {}", opcode, message);
    }
  }

  private void handleDispatch(DiscordIpcPipe pipe, JsonNode message) throws IOException {
    String event = message.path("evt").asText();
    JsonNode data = message.path("data");

    if ("ERROR".equals(event)) {
      log.warn("Discord returned an error for '{}': {}", message.path("cmd").asText(), data);
      return;
    }
    if (!"DISPATCH".equals(message.path("cmd").asText())) {
      log.debug("Discord response: {}", message);
      return;
    }

    switch (event) {
      case "READY" -> {
        ready = true;
        for (String subscription : new String[]{"ACTIVITY_JOIN", "ACTIVITY_SPECTATE", "ACTIVITY_JOIN_REQUEST"}) {
          ObjectNode subscribe = command("SUBSCRIBE");
          subscribe.put("evt", subscription);
          send(pipe, OP_FRAME, subscribe);
        }
        activityChanged.set(activity != null);
        listener.onReady(objectMapper.treeToValue(data.path("user"), DiscordUser.class));
      }
      case "ACTIVITY_JOIN" -> listener.onJoin(data.path("secret").asText());
      case "ACTIVITY_SPECTATE" -> listener.onSpectate(data.path("secret").asText());
      case "ACTIVITY_JOIN_REQUEST" -> {
        DiscordUser user = objectMapper.treeToValue(data.path("user"), DiscordUser.class);
        boolean accept = listener.onJoinRequest(user);
        sendCommand(pipe, accept ? "SEND_ACTIVITY_JOIN_INVITE" : "CLOSE_ACTIVITY_REQUEST",
                    objectMapper.createObjectNode().put("user_id", user.id()));
      }
      default -> log.debug("Ignoring Discord event: {}", message);
    }
  }

  private ObjectNode command(String command) {
    ObjectNode message = objectMapper.createObjectNode();
    message.put("cmd", command);
    message.put("nonce", UUID.randomUUID().toString());
    message.putObject("args");
    return message;
  }

  private void sendCommand(DiscordIpcPipe pipe, String command, ObjectNode args) throws IOException {
    ObjectNode message = command(command);
    message.set("args", args);
    send(pipe, OP_FRAME, message);
  }

  private void send(DiscordIpcPipe pipe, int opcode, JsonNode message) throws IOException {
    byte[] payload = objectMapper.writeValueAsBytes(message);
    pipe.write(ByteBuffer.allocate(HEADER_SIZE + payload.length)
                         .order(ByteOrder.LITTLE_ENDIAN)
                         .putInt(opcode)
                         .putInt(payload.length)
                         .put(payload)
                         .array());
  }
}
