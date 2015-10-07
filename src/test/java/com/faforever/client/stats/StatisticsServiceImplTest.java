package com.faforever.client.stats;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.stats.domain.GameStats;
import com.faforever.client.stats.domain.UnitCategory;
import com.faforever.client.stats.domain.UnitType;
import com.faforever.client.test.AbstractSpringTest;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.Unmarshaller;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@ContextConfiguration(classes = {BaseConfig.class})
public class StatisticsServiceImplTest extends AbstractSpringTest {

  @Autowired
  Unmarshaller unmarshaller;

  private StatisticsServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new StatisticsServiceImpl();
    instance.unmarshaller = unmarshaller;
  }

  @Test
  public void testParseStatistics() throws Exception {
    GameStats gameStats = instance.parseStatistics(Resources.toString(getClass().getResource("/stats/stats.xml"), StandardCharsets.UTF_8));

    assertThat(gameStats, notNullValue());
    assertThat(gameStats.getArmies(), hasSize(4));
    assertThat(gameStats.getArmies().get(0).getIndex(), is(0));
    assertThat(gameStats.getArmies().get(0).getName(), is("Downlord"));
    assertThat(gameStats.getArmies().get(0).getUnitStats(), hasSize(3));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0), notNullValue());
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getId(), is("url0001"));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getType(), is(UnitType.ACU));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getBuilt(), is(0));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getLost(), is(1));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getKilled(), is(2));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getDamagedealt(), is(1477756.00));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getDamagereceived(), is(5900.00));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getMasscost(), is(18000.00));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getMasscost(), is(18000.00));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getEnergycost(), is(5000000.00));
    assertThat(gameStats.getArmies().get(0).getUnitStats().get(0).getBuildtime(), is(6000000.00));
    assertThat(gameStats.getArmies().get(0).getSummaryStats(), hasSize(15));
    assertThat(gameStats.getArmies().get(0).getSummaryStats().get(0).getType(), is(UnitCategory.AIR));
    assertThat(gameStats.getArmies().get(0).getSummaryStats().get(0).getBuilt(), is(123));
    assertThat(gameStats.getArmies().get(0).getSummaryStats().get(0).getKilled(), is(51));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getMass(), notNullValue());
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getMass().getProduced(), is(975.86));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getMass().getConsumed(), is(4120.00));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getMass().getStorage(), is(523.13));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getEnergy(), notNullValue());
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getEnergy().getProduced(), is(6118.00));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getEnergy().getConsumed(), is(5132.31));
    assertThat(gameStats.getArmies().get(0).getEconomyStats().getEnergy().getStorage(), is(6132.51));
  }
}
