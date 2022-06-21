package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.preferences.CoturnHostPort;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Lazy
@RequiredArgsConstructor
@Service
public class CoturnService {

  private final FafApiAccessor fafApiAccessor;
  private final PreferencesService preferencesService;
  private final IceServerMapper iceServerMapper;

  public CompletableFuture<List<CoturnServer>> getActiveCoturns() {
    ElideNavigatorOnCollection<CoturnServer> navigator = ElideNavigator.of(CoturnServer.class).collection();
    return fafApiAccessor.getMany(navigator)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<CoturnServer>> getSelectedCoturns() {
    ElideNavigatorOnCollection<CoturnServer> navigator = ElideNavigator.of(CoturnServer.class).collection();
    Flux<CoturnServer> coturnServerFlux = fafApiAccessor.getMany(navigator);

    Collection<CoturnHostPort> preferredCoturnHosts = preferencesService.getPreferences().getForgedAlliance().getPreferredCoturnServers();

    return coturnServerFlux.filter(coturnServer -> preferredCoturnHosts.contains(iceServerMapper.mapToHostPort(coturnServer)))
        .switchIfEmpty(coturnServerFlux)
        .switchIfEmpty(Flux.error(new IllegalStateException("No Coturn Servers Available")))
        .collectList()
        .toFuture();
  }
}
