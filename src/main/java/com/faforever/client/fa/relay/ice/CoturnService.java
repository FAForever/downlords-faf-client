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

  public Flux<CoturnServer> getSelectedCoturns(int gameId) {
    Flux<CoturnServer> coturnServerFlux = fafApiAccessor.getApiObject("/ice/session/game/" + gameId, IceSession.class)
        .flatMapIterable(IceSession::servers);

    Set<String> preferredCoturnIds = forgedAlliancePrefs.getPreferredCoturnIds();

    return coturnServerFlux.filter(coturnServer -> preferredCoturnIds.contains(coturnServer.getId()))
        .switchIfEmpty(coturnServerFlux)
                           .switchIfEmpty(Flux.empty());
  }
}
