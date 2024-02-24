package com.faforever.client.coop;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.CoopMissionBeanBuilder;
import com.faforever.client.builders.CoopResultBeanBuilder;
import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.mapstruct.CoopMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CoopServiceTest extends ServiceTest {

  @InjectMocks
  private CoopService instance;

  @Mock
  private FafApiAccessor fafApiAccessor;

  @Spy
  private final CoopMapper coopMapper = Mappers.getMapper(CoopMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(coopMapper);

    instance = new CoopService(fafApiAccessor, coopMapper);
  }

  @Test
  public void testGetCoopMaps() throws Exception {
    CoopMissionBean coopMissionBean = CoopMissionBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(coopMapper.map(coopMissionBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getMissions()).expectNext(coopMissionBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
  }

  @Test
  public void testGetLeaderboard() throws Exception {
    CoopResultBean coopResultBean = CoopResultBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(coopMapper.map(coopResultBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    CoopMissionBean mission = CoopMissionBeanBuilder.create().defaultValues().get();
    StepVerifier.create(instance.getLeaderboard(mission, 2)).expectNext(coopResultBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("mission").eq(mission.getId())
        .and().intNum("playerCount").eq(2))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("duration", true)));
  }
}
