package com.faforever.client.filter.function;

import com.faforever.client.domain.FeaturedModBean;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.faforever.client.builders.GameBeanBuilder.create;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeaturedModFilterFunctionTest {

  private FeaturedModFilterFunction instance;

  @BeforeEach
  public void setUp() {
    instance = new FeaturedModFilterFunction();
  }

  @Test
  public void testFilter() {
    FeaturedModBean featuredMod1 = Instancio.of(FeaturedModBean.class)
                                            .set(field(FeaturedModBean::technicalName), "fafbeta")
                                            .create();
    FeaturedModBean featuredMod2 = Instancio.of(FeaturedModBean.class)
                                            .set(field(FeaturedModBean::technicalName), "faf")
                                            .create();

    List<FeaturedModBean> emptyList = Collections.emptyList();
    assertTrue(instance.apply(emptyList, create().defaultValues().featuredMod("faf").get()));
    assertTrue(instance.apply(List.of(featuredMod1), create().defaultValues().featuredMod("fafbeta").get()));
    assertFalse(instance.apply(List.of(featuredMod1, featuredMod2), create().defaultValues()
        .featuredMod("fafdevelop")
        .get()));
    assertTrue(instance.apply(List.of(featuredMod1, featuredMod2), create().defaultValues()
        .featuredMod("fafbeta")
        .get()));
  }
}