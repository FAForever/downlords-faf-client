package com.faforever.client.preferences;

import com.faforever.client.builders.VetoDataBuilder;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.VetoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MatchmakerPrefsTest extends ServiceTest {

  private MatchmakerPrefs instance;

  @BeforeEach
  public void setUp() {
    instance = new MatchmakerPrefs();
  }

  @Test
  public void testSetVetoDataAddsNew() {
    VetoData veto = VetoDataBuilder.create()
        .mapPoolMapVersionId(1)
        .vetoTokensApplied(1)
        .matchmakerQueueMapPoolId(1)
        .get();

    instance.setVetoData(veto);

    assertThat(instance.getAppliedVetoes(), hasSize(1));
    assertThat(instance.getAppliedVetoes().get(0), is(veto));
  }

  @Test
  public void testSetVetoDataUpdatesExisting() {
    VetoData veto1 = VetoDataBuilder.create()
        .mapPoolMapVersionId(1)
        .vetoTokensApplied(1)
        .matchmakerQueueMapPoolId(1)
        .get();

    instance.setVetoData(veto1);

    VetoData veto2 = VetoDataBuilder.create()
        .mapPoolMapVersionId(1)
        .vetoTokensApplied(2)
        .matchmakerQueueMapPoolId(1)
        .get();

    instance.setVetoData(veto2);

    assertThat(instance.getAppliedVetoes(), hasSize(1));
    assertThat(instance.getAppliedVetoes().get(0), is(veto2));
    assertEquals(2, instance.getAppliedVetoes().get(0).getVetoTokensApplied());
  }

  @Test
  public void testSetVetoDataPreservesOthers() {
    VetoData veto1 = VetoDataBuilder.create()
        .mapPoolMapVersionId(1)
        .vetoTokensApplied(1)
        .matchmakerQueueMapPoolId(1)
        .get();

    VetoData veto2 = VetoDataBuilder.create()
        .mapPoolMapVersionId(2)
        .vetoTokensApplied(1)
        .matchmakerQueueMapPoolId(1)
        .get();

    instance.setVetoData(veto1);
    instance.setVetoData(veto2);

    assertThat(instance.getAppliedVetoes(), hasSize(2));

    VetoData updatedVeto1 = VetoDataBuilder.create()
        .mapPoolMapVersionId(1)
        .vetoTokensApplied(3)
        .matchmakerQueueMapPoolId(1)
        .get();

    instance.setVetoData(updatedVeto1);

    assertThat(instance.getAppliedVetoes(), hasSize(2));
    assertEquals(3, instance.getAppliedVetoes().get(0).getVetoTokensApplied());
    assertEquals(1, instance.getAppliedVetoes().get(1).getVetoTokensApplied());
  }
}
