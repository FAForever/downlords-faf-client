package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.player.PlayerService;
import io.netty.buffer.ByteBuf;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpServer;

import java.net.URI;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveReplayProxyServer {

  private final ClientProperties clientProperties;
  private final PlayerService playerService;

  private DisposableServer tcpServer;

  public void stop() {
    if (tcpServer != null) {
      tcpServer.dispose();
    }
  }

  public int start(int gameId) {
    String remoteReplayServerHost = clientProperties.getReplay().getRemoteHost();

    /* A courtesy towards the replay server so we can see in logs who we're dealing with. */
    String playerName = playerService.getCurrentPlayer().getUsername();

    Flux<ByteBuf> liveReplayData = HttpClient.newConnection()
                                             .doOnConnect(
                                                            config -> log.info("Connecting to replay server at `{}`",
                                                                               config.uri()))
                                             .resolver(DefaultAddressResolverGroup.INSTANCE)
                                             .websocket()
                                             .uri(URI.create("wss://%s/%d/%s.scfareplay".formatted(
                                                            remoteReplayServerHost, gameId, playerName)))
                                             .receive()
                                             .doOnError(throwable -> log.warn("Error sending data to local replay server", throwable));

    tcpServer = TcpServer.create()
                         .doOnBound(server -> log.debug("Opening local replay server on port {}", server.port()))
                         .handle((ignored1, outbound) -> outbound.send(liveReplayData))
                         .bindNow();

    return tcpServer.port();
  }
}
