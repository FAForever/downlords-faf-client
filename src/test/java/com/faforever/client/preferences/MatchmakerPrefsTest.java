package com.faforever.client.preferences;

import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.VetoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.instancio.Instancio.of;
import static org.instancio.Select.all;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MatchmakerPrefsTest extends ServiceTest {

  private MatchmakerPrefs instance;

  @BeforeEach
  public void setUp() {
    instance = new MatchmakerPrefs();
  }

  @Test
  public void testSetVetoDataAddsNew() {
    VetoData veto = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(1, 1, 1))
        .create();

    instance.setVetoData(veto);

    assertThat(instance.getAppliedVetoes(), hasSize(1));
    assertThat(instance.getAppliedVetoes().get(0), is(veto));
  }

  @Test
  public void testSetVetoDataUpdatesExisting() {
    VetoData veto1 = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(1, 1, 1))
        .create();

    instance.setVetoData(veto1);

    VetoData veto2 = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(1, 2, 1))
        .create();

    instance.setVetoData(veto2);

    assertThat(instance.getAppliedVetoes(), hasSize(1));
    assertThat(instance.getAppliedVetoes().get(0), is(veto2));
    assertEquals(2, instance.getAppliedVetoes().get(0).getVetoTokensApplied());
  }

  @Test
  public void testSetVetoDataPreservesOthers() {
    VetoData veto1 = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(1, 1, 1))
        .create();

    VetoData veto2 = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(2, 1, 1))
        .create();

    instance.setVetoData(veto1);
    instance.setVetoData(veto2);

    assertThat(instance.getAppliedVetoes(), hasSize(2));

    VetoData updatedVeto1 = of(VetoData.class)
        .supply(all(VetoData.class), () -> new VetoData(1, 3, 1))
        .create();

    instance.setVetoData(updatedVeto1);

    assertThat(instance.getAppliedVetoes(), hasSize(2));
    assertEquals(3, instance.getAppliedVetoes().get(0).getVetoTokensApplied());
    assertEquals(1, instance.getAppliedVetoes().get(1).getVetoTokensApplied());
  }
}
