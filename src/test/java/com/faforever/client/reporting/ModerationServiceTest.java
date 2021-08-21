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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModerationServiceTest extends ServiceTest {
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private PlayerService playerService;

  private ModerationService instance;
  private PlayerBean player;

  private final ModerationReportMapper moderationReportMapper = Mappers.getMapper(ModerationReportMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(moderationReportMapper);
    instance = new ModerationService(fafApiAccessor, playerService, moderationReportMapper);

    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    when(playerService.getCurrentPlayer()).thenReturn(player);
  }

  @Test
  public void testGetModerationReports() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    instance.getModerationReports();
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().intNum("reporter.id").eq(player.getId())
    )));
  }

  @Test
  public void testPostModerationReport() {
    ModerationReportBean report = ModerationReportBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.post(any(), any())).thenReturn(Mono.empty());
    instance.postModerationReport(report);
    verify(fafApiAccessor).post(any(), eq(moderationReportMapper.map(report, new CycleAvoidingMappingContext())));
  }
}
