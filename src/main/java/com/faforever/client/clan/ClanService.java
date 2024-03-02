package com.faforever.client.clan;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.api.Clan;
import com.faforever.client.mapstruct.ClanMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Lazy
@Service
@RequiredArgsConstructor
public class ClanService {

  private final FafApiAccessor fafApiAccessor;
  private final ClanMapper clanMapper;

  @Cacheable(value = CacheNames.CLAN, sync = true)
  public Mono<Clan> getClanByTag(String tag) {
    if (tag == null) {
      return Mono.empty();
    }

    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Clan> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Clan.class).collection().setFilter(qBuilder().string("tag").eq(tag)).pageSize(1);
    return fafApiAccessor.getMany(navigator)
        .next().map(dto -> clanMapper.map(dto, new CycleAvoidingMappingContext())).cache();
  }
}


