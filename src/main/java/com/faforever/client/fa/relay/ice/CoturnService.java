package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.IceServer;
import com.faforever.client.api.IceServerResponse;
import com.faforever.client.api.IceSession;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.commons.api.dto.CoturnServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;


@Lazy
@RequiredArgsConstructor
@Service
public class CoturnService {

  private final FafApiAccessor fafApiAccessor;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  public Flux<IceServer> getActiveCoturns() {
    return fafApiAccessor.getApiObject("/ice/server", IceServerResponse.class)
                         .flatMapIterable(IceServerResponse::servers)
                         .switchIfEmpty(Flux.empty());
  }

  public Mono<IceSession> getIceSession(int gameId) {
    Set<String> preferredCoturns = forgedAlliancePrefs.getPreferredCoturnIds();
    return fafApiAccessor.getApiObject("/ice/session/game/" + gameId, IceSession.class).map(session -> {
      List<CoturnServer> preferredServers = session.servers()
                                                   .stream()
                                                   .filter(
                                                       coturnServer -> preferredCoturns.contains(coturnServer.getId()))
                                                   .toList();

      if (preferredServers.isEmpty()) {
        return session;
      } else {
        return new IceSession(session.id(), session.forceRelay(), preferredServers);
      }
    });
  }

}
