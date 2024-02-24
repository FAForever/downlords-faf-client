package com.faforever.client.reporting;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.ModerationReportMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class ModerationService {
  private final FafApiAccessor fafApiAccessor;
  private final PlayerService playerService;
  private final ModerationReportMapper moderationReportMapper;

  @Cacheable(value = CacheNames.MODERATION_REPORTS, sync = true)
  public Flux<ModerationReportBean> getModerationReports() {
    ElideNavigatorOnCollection<ModerationReport> navigator = ElideNavigator.of(ModerationReport.class).collection()
        .setFilter(qBuilder().intNum("reporter.id").eq(playerService.getCurrentPlayer().getId())
        .and().instant("createTime").after(OffsetDateTime.now().minusYears(1).toInstant(), false))
        .addSortingRule("createTime", false);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> moderationReportMapper.map(dto, new CycleAvoidingMappingContext())).cache();
  }

  @CacheEvict(value = CacheNames.MODERATION_REPORTS)
  public Mono<ModerationReportBean> postModerationReport(ModerationReportBean report) {
    ModerationReport reportDto = moderationReportMapper.map(report, new CycleAvoidingMappingContext());
    ElideNavigatorOnCollection<ModerationReport> navigator = ElideNavigator.of(ModerationReport.class).collection();
    return fafApiAccessor.post(navigator, reportDto)
                         .map(dto -> moderationReportMapper.map(dto, new CycleAvoidingMappingContext()));
  }
}
