package com.faforever.client.clan;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.Clan;
import com.faforever.client.mapstruct.ClanMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.instancio.Instancio;
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

import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.instancio.Select.field;
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
    Clan clan = Instancio.of(Clan.class).set(field(Clan::members), List.of()).create();
    Flux<ElideEntity> resultFlux = Flux.just(clanMapper.map(clan));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getClanByTag("test")).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("tag").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
  }
}
