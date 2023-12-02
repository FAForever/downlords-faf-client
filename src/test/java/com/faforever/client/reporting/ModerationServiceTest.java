package com.faforever.client.reporting;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.ModerationReportBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ModerationReportMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.elide.ElideEntity;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModerationServiceTest extends ServiceTest {
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private PlayerService playerService;

  @InjectMocks
  private ModerationService instance;
  private PlayerBean player;

  @Spy
  private ModerationReportMapper moderationReportMapper = Mappers.getMapper(ModerationReportMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(moderationReportMapper);

    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
  }

  @Test
  public void testGetModerationReports() {
    ModerationReportBean report = ModerationReportBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(moderationReportMapper.map(report, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    List<ModerationReportBean> results = instance.getModerationReports().join();
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().intNum("reporter.id").eq(player.getId())
    )));
    assertThat(results, contains(report));
  }

  @Test
  public void testPostModerationReport() {
    ModerationReportBean report = ModerationReportBeanBuilder.create().defaultValues().get();
    ModerationReport moderationReport = moderationReportMapper.map(report, new CycleAvoidingMappingContext());
    Mono<ElideEntity> resultMono = Mono.just(moderationReport);
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);
    ModerationReportBean result = instance.postModerationReport(report).join();
    verify(fafApiAccessor).post(any(), eq(moderationReport));
    assertThat(result, CoreMatchers.is(report));
  }
}
