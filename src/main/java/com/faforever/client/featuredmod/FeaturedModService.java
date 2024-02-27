package com.faforever.client.featuredmod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.FeaturedModMapper;
import com.faforever.client.patch.GameUpdater;
import com.faforever.commons.api.dto.FeaturedMod;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static java.lang.String.format;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class FeaturedModService {

  private final FafApiAccessor fafApiAccessor;
  private final FeaturedModMapper featuredModMapper;
  private final GameUpdater gameUpdater;

  public CompletableFuture<Void> updateFeaturedMod(String featuredModName, Map<String, Integer> featuredModFileVersions,
                                                   Integer baseVersion, boolean forReplays) {
    return gameUpdater.update(featuredModName, featuredModFileVersions, baseVersion, forReplays);
  }

  public CompletableFuture<Void> updateFeaturedModToLatest(String featuredModName, boolean forReplays) {
    return updateFeaturedMod(featuredModName, null, null, forReplays);
  }

  @Cacheable(value = CacheNames.FEATURED_MOD_FILES, sync = true)
  public Flux<FeaturedModFile> getFeaturedModFiles(FeaturedModBean featuredMod, Integer version) {
    String endpoint = format("/featuredMods/%s/files/%s", featuredMod.id(),
                             Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return fafApiAccessor.getMany(FeaturedModFile.class, endpoint, fafApiAccessor.getMaxPageSize(), Map.of()).cache();
  }

  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public Mono<FeaturedModBean> getFeaturedMod(String technicalName) {
    ElideNavigatorOnCollection<FeaturedMod> navigator = ElideNavigator.of(FeaturedMod.class)
                                                                      .collection()
                                                                      .setFilter(qBuilder().string("technicalName")
                                                                                           .eq(technicalName))
                                                                      .addSortingRule("order", true)
                                                                      .pageSize(1);
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .switchIfEmpty(
                             Mono.error(new IllegalArgumentException("Not a valid featured mod: " + technicalName)))
                         .map(dto -> featuredModMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public Flux<FeaturedModBean> getFeaturedMods() {
    ElideNavigatorOnCollection<FeaturedMod> navigator = ElideNavigator.of(FeaturedMod.class)
                                                                      .collection()
                                                                      .setFilter(qBuilder().bool("visible").isTrue())
                                                                      .addSortingRule("order", true)
                                                                      .pageSize(50);
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> featuredModMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }
}
