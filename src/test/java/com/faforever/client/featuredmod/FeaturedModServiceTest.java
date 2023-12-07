package com.faforever.client.featuredmod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.FeaturedModMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.elide.ElideEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FeaturedModServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Spy
  private FeaturedModMapper featuredModMapper = Mappers.getMapper(FeaturedModMapper.class);

  @InjectMocks
  private FeaturedModService instance;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(featuredModMapper);
  }

  @Test
  public void testGetFeaturedFiles() {
    when(fafApiAccessor.getMaxPageSize()).thenReturn(100);
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(eq(FeaturedModFile.class), anyString(), anyInt(), any())).thenReturn(
        Flux.just(new FeaturedModFile()));

    instance.getFeaturedModFiles(featuredMod, 0);
    verify(fafApiAccessor).getMany(eq(FeaturedModFile.class),
                                   eq(String.format("/featuredMods/%s/files/%s", featuredMod.getId(), 0)), eq(100),
                                   any());
  }

  @Test
  public void testGetFeaturedMod() {
    FeaturedModBean featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(featuredModMapper.map(featuredMod, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    FeaturedModBean result = instance.getFeaturedMod("test").block();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("test"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("order", true)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1)));
    assertThat(result, is(featuredMod));
  }
}
