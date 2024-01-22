package com.faforever.client.clan;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.mapstruct.ClanMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClanServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;

  @Spy
  private ClanMapper clanMapper = Mappers.getMapper(ClanMapper.class);
  @InjectMocks
  private ClanService instance;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(clanMapper);
  }

  @Test
  public void testGetClanByTag() throws Exception {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(clanMapper.map(clan, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getClanByTag("test")).expectNext(clan).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("tag").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
  }
}
