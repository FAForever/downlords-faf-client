package com.faforever.client.reporting;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.ModerationReport;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ModerationReportMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
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
  private PlayerInfo player;

  @Spy
  private ModerationReportMapper moderationReportMapper = Mappers.getMapper(ModerationReportMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(moderationReportMapper);

    player = PlayerInfoBuilder.create().defaultValues().username("junit").get();

    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
  }

  @Test
  public void testGetModerationReports() {
    ModerationReport report = Instancio.create(ModerationReport.class);
    Flux<ElideEntity> resultFlux = Flux.just(moderationReportMapper.map(report));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getModerationReports()).expectNextCount(1).verifyComplete();
    ;
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().intNum("reporter.id").eq(player.getId())
    )));
  }

  @Test
  public void testPostModerationReport() {
    ModerationReport report = Instancio.create(ModerationReport.class);
    com.faforever.commons.api.dto.ModerationReport moderationReport = moderationReportMapper.map(report);
    Mono<ElideEntity> resultMono = Mono.just(moderationReport);
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);
    StepVerifier.create(instance.postModerationReport(report)).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(any(), eq(moderationReport));
  }
}
