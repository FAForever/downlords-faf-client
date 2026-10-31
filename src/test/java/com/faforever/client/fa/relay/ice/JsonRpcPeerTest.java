package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.relay.ice.JsonRpcPeer.RemoteException;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class JsonRpcPeerTest extends ServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private IceAdapterCallbacks callbacks;

  private ServerSocket serverSocket;
  private Socket remote;
  private BufferedReader remoteReader;
  private JsonRpcPeer instance;

  @BeforeEach
  public void setUp() throws Exception {
    serverSocket = new ServerSocket(0);
    instance = JsonRpcPeer.connect("localhost", serverSocket.getLocalPort(), callbacks);
    remote = serverSocket.accept();
    remoteReader = new BufferedReader(new InputStreamReader(remote.getInputStream(), StandardCharsets.UTF_8));
  }

  @AfterEach
  public void tearDown() throws Exception {
    instance.close();
    remote.close();
    serverSocket.close();
  }

  @Test
  public void sendRequestWritesRequestAndReturnsConvertedResult() throws Exception {
    CompletableFuture<Object> result = instance.sendRequest("status", List.of(), Map.class);

    JsonNode request = objectMapper.readTree(remoteReader.readLine());
    assertThat(request.get("jsonrpc").asText(), is("2.0"));
    assertThat(request.get("method").asText(), is("status"));
    assertThat(request.get("params").size(), is(0));

    sendFromRemote("{\"jsonrpc\":\"2.0\",\"id\":" + request.get("id") + ",\"result\":{\"version\":\"1.0\"}}");

    assertThat(result.get(1, TimeUnit.SECONDS), is(Map.of("version", "1.0")));
  }

  @Test
  public void sendRequestSerializesParams() throws Exception {
    instance.sendRequest("setIceServers", List.of(List.of(Map.of("urls", List.of("turn:example.com")))), void.class);

    JsonNode request = objectMapper.readTree(remoteReader.readLine());
    assertThat(request.get("params").toString(), is("[[{\"urls\":[\"turn:example.com\"]}]]"));
  }

  @Test
  public void errorResponseFailsRequest() throws Exception {
    CompletableFuture<Object> result = instance.sendRequest("hostGame", List.of("map"), void.class);

    JsonNode request = objectMapper.readTree(remoteReader.readLine());
    sendFromRemote("{\"jsonrpc\":\"2.0\",\"id\":" + request.get("id") + ",\"error\":{\"code\":-32601,\"message\":\"Method Not Found\"}}");

    CompletionException exception = assertThrows(CompletionException.class, result::join);
    assertThat(exception.getCause(), instanceOf(RemoteException.class));
  }

  @Test
  public void notificationsAreDispatchedToHandler() throws Exception {
    sendFromRemote("""
        {"jsonrpc":"2.0","method":"onConnectionStateChanged","params":["connected"]}
        {"jsonrpc":"2.0","method":"onGpgNetMessageReceived","params":["GameState",["Idle"]]}
        {"jsonrpc":"2.0","method":"onIceMsg","params":[1,2,{"type":"offer"}]}
        {"jsonrpc":"2.0","method":"onConnected","params":[1,2,true]}
        """);

    verify(callbacks, timeout(1000)).onConnectionStateChanged("connected");
    verify(callbacks, timeout(1000)).onGpgNetMessageReceived("GameState", List.of("Idle"));
    verify(callbacks, timeout(1000)).onIceMsg(1L, 2L, Map.of("type", "offer"));
    verify(callbacks, timeout(1000)).onConnected(1L, 2L, true);
  }

  @Test
  public void messagesWithoutNewlineAreParsed() throws Exception {
    sendFromRemote("{\"jsonrpc\":\"2.0\",\"method\":\"onConnectionStateChanged\",\"params\":[\"a\"]}"
        + "{\"jsonrpc\":\"2.0\",\"method\":\"onConnectionStateChanged\",\"params\":[\"b\"]}");

    verify(callbacks, timeout(1000)).onConnectionStateChanged("a");
    verify(callbacks, timeout(1000)).onConnectionStateChanged("b");
  }

  @Test
  public void requestIsAnswered() throws Exception {
    sendFromRemote("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"onConnectionStateChanged\",\"params\":[\"connected\"]}");

    JsonNode response = objectMapper.readTree(remoteReader.readLine());
    assertThat(response.get("id").asInt(), is(7));
    assertThat(response.has("result"), is(true));
    verify(callbacks).onConnectionStateChanged("connected");
  }

  @Test
  public void unknownMethodIsAnsweredWithError() throws Exception {
    sendFromRemote("{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"doesNotExist\",\"params\":[]}");

    JsonNode response = objectMapper.readTree(remoteReader.readLine());
    assertThat(response.get("id").asInt(), is(8));
    assertThat(response.get("error").get("code").asInt(), is(-32601));
  }

  @Test
  public void connectionLossFailsPendingRequests() throws Exception {
    CompletableFuture<Object> result = instance.sendRequest("status", List.of(), Map.class);
    remoteReader.readLine();

    remote.close();

    CompletionException exception = assertThrows(CompletionException.class, result::join);
    assertThat(exception.getCause(), instanceOf(IOException.class));
    awaitDead();
  }

  private void awaitDead() throws InterruptedException {
    for (int i = 0; i < 100 && instance.isAlive(); i++) {
      Thread.sleep(10);
    }
    assertThat(instance.isAlive(), is(false));
  }

  private void sendFromRemote(String data) throws IOException {
    OutputStream outputStream = remote.getOutputStream();
    outputStream.write(data.getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
  }
}
