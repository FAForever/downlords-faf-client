package com.faforever.client.replay;

import com.faforever.client.remote.HmacAccess;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpServer;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveReplayProxyServer {

  @Qualifier("userWebClient")
  private final ObjectFactory<WebClient> userWebClientFactory;

  private DisposableServer tcpServer;

  public void stop() {
    if (tcpServer != null) {
      tcpServer.dispose();
    }
  }

  public int start() {
    tcpServer = TcpServer.create()
                         .doOnBound(server -> log.debug("Opening local live replay server on port {}", server.port()))
                         .doOnUnbound(server -> log.debug("Closing local live replay server on port {}", server.port()))
                         .handle((inbound, outbound) -> userWebClientFactory.getObject()
                                                                            .get()
                                                                            .uri("/replay/access")
                                                                            .retrieve()
                                                                            .bodyToMono(HmacAccess.class)
                                                                            .map(HmacAccess::accessUrl)
                                                                            .flatMapMany(
                                                                                url -> HttpClient.newConnection()
                                                                                                 .doOnConnect(
                                                                                                     config -> log.info(
                                                                                                         "Connecting to replay server at `{}`",
                                                                                                         config.uri()))
                                                                                                 .resolver(
                                                                                                     DefaultAddressResolverGroup.INSTANCE)
                                                                                                 .websocket()
                                                                                                 .uri(url)
                                                                                                 .handle(
                                                                                                     ((websocketInbound, websocketOutbound) -> Flux.merge(
                                                                                                         outbound.sendByteArray(
                                                                                                             websocketInbound.receive()
                                                                                                                             .asByteArray()),
                                                                                                         websocketOutbound.sendByteArray(
                                                                                                             inbound.receive()
                                                                                                                    .asByteArray()))))
                                                                                                 .doOnError(
                                                                                                     throwable -> log.warn(
                                                                                                         "Error sending data to local replay server",
                                                                                                         throwable))))
                         .bindNow();

    return tcpServer.port();
  }
}
