package com.faforever.client.clan;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.mapstruct.ClanMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClanServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;

  private ClanMapper clanMapper = Mappers.getMapper(ClanMapper.class);
  private ClanService instance;
  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(clanMapper);
    instance = new ClanService(fafApiAccessor, clanMapper);
  }

  @Test
  public void testGetClanByTag() throws Exception {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(clanMapper.map(clan, new CycleAvoidingMappingContext())));
    assertEquals(clan, instance.getClanByTag("test").join().get());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("tag").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
  }
}
