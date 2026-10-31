package com.faforever.client.fa.relay.ice;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal bidirectional JSON-RPC 2.0 peer over a TCP socket, as spoken by the ICE adapter. Outgoing calls are sent as
 * requests; incoming requests and notifications are dispatched to the public methods of a handler object, matched by
 * name and parameter count.
 */
@Slf4j
class JsonRpcPeer implements AutoCloseable {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final int ERROR_METHOD_NOT_FOUND = -32601;
  private static final int ERROR_INTERNAL = -32603;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
  private final AtomicInteger nextId = new AtomicInteger();
  private final Socket socket;
  private final OutputStream outputStream;
  private final Object handler;
  private final Thread readerThread;

  private JsonRpcPeer(Socket socket, Object handler) throws IOException {
    this.socket = socket;
    this.outputStream = socket.getOutputStream();
    this.handler = handler;
    this.readerThread = Thread.ofPlatform().name("json-rpc-peer").daemon().unstarted(this::readMessages);
  }

  static JsonRpcPeer connect(String host, int port, Object handler) throws IOException {
    JsonRpcPeer peer = new JsonRpcPeer(new Socket(host, port), handler);
    peer.readerThread.start();
    return peer;
  }

  boolean isAlive() {
    return readerThread.isAlive() && !socket.isClosed();
  }

  /**
   * Sends a request and returns a future of its result, converted to {@code resultType}. The future fails if the remote
   * side responds with an error, the connection is lost or no response arrives in time.
   */
  CompletableFuture<Object> sendRequest(String method, List<?> params, Type resultType) {
    int id = nextId.incrementAndGet();
    CompletableFuture<JsonNode> response = new CompletableFuture<>();
    pendingRequests.put(id, response);
    response.orTimeout(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        .whenComplete((result, throwable) -> pendingRequests.remove(id));

    ObjectNode request = objectMapper.createObjectNode()
        .put("jsonrpc", "2.0")
        .put("method", method)
        .put("id", id);
    request.set("params", objectMapper.valueToTree(params));
    try {
      write(request);
    } catch (IOException e) {
      response.completeExceptionally(e);
    }

    return response.thenApply(result -> objectMapper.convertValue(result, objectMapper.constructType(resultType)));
  }

  @Override
  public void close() {
    try {
      socket.close();
    } catch (IOException e) {
      log.warn("Could not close JSON-RPC socket", e);
    }
  }

  private void write(JsonNode message) throws IOException {
    byte[] bytes = objectMapper.writeValueAsBytes(message);
    synchronized (outputStream) {
      outputStream.write(bytes);
      outputStream.write('\n');
      outputStream.flush();
    }
  }

  private void readMessages() {
    try (JsonParser parser = objectMapper.createParser(socket.getInputStream())) {
      while (parser.nextToken() != null) {
        JsonNode message = parser.readValueAsTree();
        try {
          handleMessage(message);
        } catch (RuntimeException e) {
          log.warn("Could not handle JSON-RPC message: {}", message, e);
        }
      }
    } catch (IOException e) {
      log.info("JSON-RPC connection closed ({})", e.getMessage());
    } finally {
      close();
      IOException connectionClosed = new IOException("JSON-RPC connection closed");
      pendingRequests.values().forEach(response -> response.completeExceptionally(connectionClosed));
    }
  }

  private void handleMessage(JsonNode message) {
    if (message.has("method")) {
      handleCall(message);
      return;
    }

    CompletableFuture<JsonNode> response = pendingRequests.get(message.path("id").asInt());
    if (response == null) {
      log.debug("Ignoring JSON-RPC response without pending request: {}", message);
    } else if (message.hasNonNull("error")) {
      response.completeExceptionally(new RemoteException(message.get("error")));
    } else {
      response.complete(message.get("result"));
    }
  }

  private void handleCall(JsonNode message) {
    String methodName = message.get("method").asText();
    JsonNode id = message.get("id");
    List<JsonNode> params = message.path("params") instanceof ArrayNode array ? array.valueStream().toList() : List.of();

    Optional<Method> method = Arrays.stream(handler.getClass().getMethods())
        .filter(candidate -> candidate.getName().equals(methodName))
        .filter(candidate -> candidate.getParameterCount() == params.size())
        .findFirst();

    if (method.isEmpty()) {
      log.warn("No handler method for JSON-RPC call: {}", message);
      respondWithError(id, ERROR_METHOD_NOT_FOUND, "Method not found");
      return;
    }

    Type[] parameterTypes = method.get().getGenericParameterTypes();
    Object[] args = new Object[params.size()];
    for (int i = 0; i < args.length; i++) {
      args[i] = objectMapper.convertValue(params.get(i), objectMapper.constructType(parameterTypes[i]));
    }

    Object result;
    try {
      result = method.get().invoke(handler, args);
    } catch (IllegalAccessException | InvocationTargetException e) {
      log.warn("JSON-RPC handler method failed: {}", message, e);
      respondWithError(id, ERROR_INTERNAL, "Internal error");
      return;
    }

    if (id != null) {
      ObjectNode response = objectMapper.createObjectNode().put("jsonrpc", "2.0");
      response.set("id", id);
      response.set("result", objectMapper.valueToTree(result));
      writeQuietly(response);
    }
  }

  private void respondWithError(JsonNode id, int code, String errorMessage) {
    if (id == null) {
      return;
    }
    ObjectNode response = objectMapper.createObjectNode().put("jsonrpc", "2.0");
    response.set("id", id);
    response.putObject("error").put("code", code).put("message", errorMessage);
    writeQuietly(response);
  }

  private void writeQuietly(JsonNode message) {
    try {
      write(message);
    } catch (IOException e) {
      log.warn("Could not send JSON-RPC message: {}", message, e);
    }
  }

  static class RemoteException extends RuntimeException {
    RemoteException(JsonNode error) {
      super(error.path("message").asText() + " (" + error.path("code").asInt() + ")");
    }
  }
}
