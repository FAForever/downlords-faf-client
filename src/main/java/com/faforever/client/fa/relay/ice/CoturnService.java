package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.IceServer;
import com.faforever.client.api.IceSession;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.commons.api.dto.CoturnServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Lazy
@RequiredArgsConstructor
@Service
public class CoturnService {

  private final FafApiAccessor fafApiAccessor;
  private final IceServerMapper iceServerMapper;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  public CompletableFuture<List<IceServer>> getActiveCoturns() {
    return fafApiAccessor.getIceServers()
        .switchIfEmpty(Flux.error(new IllegalStateException("No Coturn Servers Available")))
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<CoturnServer>> getSelectedCoturns(int gameId) {
    Flux<CoturnServer> coturnServerFlux = fafApiAccessor.getIceSession(gameId)
        .flatMapIterable(IceSession::servers);

    Set<String> preferredCoturnIds = forgedAlliancePrefs.getPreferredCoturnIds();

    return coturnServerFlux.filter(coturnServer -> preferredCoturnIds.contains(coturnServer.getId()))
        .switchIfEmpty(coturnServerFlux)
        .switchIfEmpty(Flux.error(new IllegalStateException("No Coturn Servers Available")))
        .collectList()
        .toFuture();
  }
}
