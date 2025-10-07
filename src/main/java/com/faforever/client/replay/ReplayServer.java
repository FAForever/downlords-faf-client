package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.game.GameService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.HmacAccess;
import com.faforever.client.update.Version;
import com.faforever.client.user.LoginService;
import com.faforever.commons.replay.ReplayMetadata;
import com.google.common.primitives.Bytes;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class ReplayServer {

  /**
   * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
   */
  private static final byte[] LIVE_REPLAY_PREFIX = new byte[]{'P', '/'};

  private final ClientProperties clientProperties;
  private final LoginService loginService;
  private final ReplayFileWriter replayFileWriter;
  private final PlayerService playerService;
  private final GameService gameService;
  @Qualifier("userWebClient")
  private final ObjectFactory<WebClient> userWebClientFactory;

  private DisposableServer tcpServer;
  private Connection remoteReplayConnection;

  /**
   * Returns the current millis the same way as python does since this is what's stored in the replay files *yay*.
   */
  private static double pythonTime() {
    return System.currentTimeMillis() / 1000d;
  }

  public void stop() {
    if (tcpServer != null) {
      tcpServer.dispose();
    }

    if (remoteReplayConnection != null) {
      remoteReplayConnection.dispose();
    }
  }

  public CompletableFuture<Integer> start(int gameId) {
    ReplayMetadata replayInfo = initReplayInfo(gameId);

    return TcpServer.create()
                    .doOnBound(server -> {
                      log.debug("Opening local replay server on port {}", server.port());
                      this.tcpServer = server;
                    })
                    .doOnUnbound(server -> log.debug("Closing local replay server on port {}", server.port()))
                    .handle((inbound, _) -> {
                      ByteArrayOutputStream replayData = new ByteArrayOutputStream();
                      Flux<byte[]> incomingReplayData = inbound.receive().asByteArray().replay().refCount();

                      Mono<Void> remoteReplayData = userWebClientFactory.getObject()
                                                                        .get()
                                                                        .uri("/replay/access")
                                                                        .retrieve()
                                                                        .bodyToMono(HmacAccess.class)
                                                                        .map(HmacAccess::accessUrl)
                                                                        .flatMap(url -> HttpClient.newConnection()
                                                                                                  .doOnConnect(
                                                                                                      config -> log.info(
                                                                                                          "Connecting to replay server at `{}`",
                                                                                                          config.uri()))
                                                                                                  .resolver(
                                                                                                      DefaultAddressResolverGroup.INSTANCE)
                                                                                                  .doOnConnected(
                                                                                                      connection -> this.remoteReplayConnection = connection)
                                                                                                  .websocket()
                                                                                                  .uri(url)
                                                                                                  .handle(
                                                                                                      (_, outbound) -> outbound.sendByteArray(
                                                                                                          incomingReplayData))
                                                                                                  .then()
                                                                                                  .doOnError(
                                                                                                      throwable -> log.warn(
                                                                                                          "Error sending data to remote replay server",
                                                                                                          throwable))
                                                                                                  .onErrorComplete());

                      Mono<Void> localReplayData = incomingReplayData.doOnNext(buffer -> {
                                                                       if (replayData.size() == 0 && Bytes.indexOf(buffer, LIVE_REPLAY_PREFIX) != -1) {
                                                                         int dataBeginIndex = Bytes.indexOf(buffer, (byte) 0x00) + 1;
                                                                         replayData.write(buffer, dataBeginIndex, buffer.length - dataBeginIndex);
                                                                       } else {
                                                                         replayData.writeBytes(buffer);
                                                                       }
                                                                     })
                                                                     .then()
                                                                     .doOnError(
                                                                         throwable -> log.warn("Error in replay server",
                                                                                               throwable))
                                                                     .doFinally(signalType -> {
                                                                       if (signalType == SignalType.ON_ERROR) {
                                                                         return;
                                                                       }

                                                                       log.info(
                                                                           "FAF disconnected, writing replay data to file");
                                                                       GameInfo game = gameService.getByUid(gameId)
                                                                                                  .orElseThrow();
                                                                       finishReplayInfo(game, replayInfo);
                                                                       try {
                                                                         replayFileWriter.writeReplayDataToFile(
                                                                             replayData, replayInfo);
                                                                       } catch (IOException e) {
                                                                         log.warn("Unable to write replay data to file",
                                                                                  e);
                                                                       }
                                                                     });

                      return Mono.when(remoteReplayData, localReplayData);
                    })
                    .bind()
                    .map(DisposableServer::port)
                    .toFuture();
  }

  private ReplayMetadata initReplayInfo(int uid) {
    ReplayMetadata replayInfo = new ReplayMetadata();
    replayInfo.setUid(uid);
    replayInfo.setLaunchedAt(pythonTime());
    replayInfo.setVersionInfo(new HashMap<>());
    replayInfo.getVersionInfo().put("lobby", String.format("dfaf-%s", Version.getCurrentVersion()));
    return replayInfo;
  }

  private void finishReplayInfo(GameInfo game, ReplayMetadata replayInfo) {
    Map<String, List<String>> teamStrings = game.getTeams()
                                                .entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(String::valueOf, entry -> entry.getValue()
                                                                                                         .stream()
                                                                                                         .map(
                                                                                                             playerService::getPlayerByIdIfOnline)
                                                                                                         .flatMap(
                                                                                                             Optional::stream)
                                                                                                         .map(
                                                                                                             PlayerInfo::getUsername)
                                                                                                         .collect(
                                                                                                             Collectors.toList())));

    replayInfo.setHost(game.getHost());
    replayInfo.setUid(game.getId());
    replayInfo.setTitle(game.getTitle());
    replayInfo.setMapname(game.getMapFolderName());
    replayInfo.setVictoryCondition(game.getVictoryCondition());
    replayInfo.setFeaturedMod(game.getFeaturedMod());
    replayInfo.setMaxPlayers(game.getMaxPlayers());
    replayInfo.setNumPlayers(game.getNumActivePlayers());
    replayInfo.setSimMods(game.getSimMods());
    replayInfo.setTeams(teamStrings);
    replayInfo.setFeaturedModVersions(Map.of());
    replayInfo.setGameEnd(pythonTime());
    replayInfo.setRecorder(loginService.getUsername());
    // TODO: Use enum when setter is fixed in java commons
    replayInfo.setState("closed");
    replayInfo.setComplete(true);
  }
}
