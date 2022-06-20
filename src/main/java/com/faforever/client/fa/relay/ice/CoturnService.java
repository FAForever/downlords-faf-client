package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

  @Cacheable(value = CacheNames.COTURN, sync = true)
  public CompletableFuture<List<CoturnServer>> getActiveCoturns() {
    ElideNavigatorOnCollection<CoturnServer> navigator = ElideNavigator.of(CoturnServer.class).collection();
    return fafApiAccessor.getMany(navigator)
        .filter(CoturnServer::getActive)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<CoturnServer>> getSelectedCoturns() {
    ElideNavigatorOnCollection<CoturnServer> navigator = ElideNavigator.of(CoturnServer.class).collection();
    Flux<CoturnServer> coturnServerFlux = fafApiAccessor.getMany(navigator).filter(CoturnServer::getActive);

    Collection<String> preferredCoturnHosts = preferencesService.getPreferences().getForgedAlliance().getPreferredCoturnServers();

    return coturnServerFlux.filter(coturnServer -> preferredCoturnHosts.contains(coturnServer.getHost()))
        .switchIfEmpty(coturnServerFlux)
        .collectList()
        .toFuture();
  }
}
