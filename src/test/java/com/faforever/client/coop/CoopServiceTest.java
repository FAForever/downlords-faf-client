package com.faforever.client.coop;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.CoopMissionBeanBuilder;
import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.mapstruct.CoopMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import reactor.core.publisher.Flux;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CoopServiceTest extends ServiceTest {

  private CoopService instance;

  @Mock
  private FafApiAccessor fafApiAccessor;

  private final CoopMapper coopMapper = Mappers.getMapper(CoopMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(coopMapper);

    instance = new CoopService(fafApiAccessor, coopMapper);
  }

  @Test
  public void testGetCoopMaps() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    instance.getMissions();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
  }

  @Test
  public void testGetLeaderboard() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    CoopMissionBean mission = CoopMissionBeanBuilder.create().defaultValues().get();
    instance.getLeaderboard(mission, 2);
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("mission").eq(mission.getId())
        .and().intNum("playerCount").eq(2))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("duration", true)));
  }
}
