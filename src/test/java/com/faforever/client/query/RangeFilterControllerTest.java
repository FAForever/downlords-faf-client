package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.IntegerProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.control.MenuButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RangeFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private final double max = 100;
  private final double min = 0;
  private final double increment = 1.0;
  private RangeFilterController instance;
  @Mock
  private I18n i18n;
  @Mock
  private InvalidationListener queryListener;

  @Before
  public void setUp() throws Exception {
    instance = new RangeFilterController(i18n);

    loadFxml("theme/vault/search/rangeFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
    instance.setMin(min);
    instance.setMax(max);
    instance.setIncrement(increment);
    instance.setSnapToTicks(true);
    instance.setTickUnit(increment);
  }

  @Test
  public void testTextBinding() throws Exception {
    instance.rangeSlider.setLowValue(10.0);
    instance.rangeSlider.setHighValue(90.0);

    assertEquals(instance.lowValue.getText(), "10");
    assertEquals(instance.highValue.getText(), "90");

    instance.rangeSlider.setLowValue(min);
    instance.rangeSlider.setHighValue(max);

    assertEquals(instance.lowValue.getText(), "");
    assertEquals(instance.highValue.getText(), "");
  }

  @Test
  public void testSliderBinding() throws Exception {
    instance.lowValue.setText("20");
    instance.highValue.setText("80");

    assertEquals(instance.rangeSlider.getLowValue(), 20, 0);
    assertEquals(instance.rangeSlider.getHighValue(), 80, 0);

    instance.lowValue.setText("a");
    instance.highValue.setText("a");

    assertEquals(min, instance.rangeSlider.getLowValue(), 0);
    assertEquals(max, instance.rangeSlider.getHighValue(), 0);
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.rangeSlider.setLowValue(10.0);
    instance.rangeSlider.setHighValue(90.0);
    instance.lowValue.setText("20");
    instance.highValue.setText("80");
    verify(queryListener, times(12)).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.rangeSlider.setLowValue(10.0);
    instance.rangeSlider.setHighValue(90.0);
    instance.clear();

    assertEquals(instance.rangeSlider.getLowValue(), min, 0);
    assertEquals(instance.rangeSlider.getHighValue(), max, 0);
  }

  @Test
  public void testSetTitle() throws Exception {
    when(i18n.get(anyString(), anyString(), anyString(), anyString())).thenReturn("Test");
    instance.setTitle("Test");

    assertTrue(instance.menu.textProperty().isBound());
    assertEquals("Test", instance.menu.getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof MenuButton);
  }

  @Test
  public void testGetConditionMaxRange() throws Exception {
    assertTrue(instance.getCondition().isEmpty());
    assertFalse(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionMaximum() throws Exception {
    IntegerProperty property = new QBuilder<>().intNum(propertyName);
    instance.rangeSlider.setHighValue(50.0);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.lte(50).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionMinimum() throws Exception {
    IntegerProperty property = new QBuilder<>().intNum(propertyName);
    instance.rangeSlider.setLowValue(50.0);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.gte(50).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionRange() throws Exception {
    instance.rangeSlider.setLowValue(50.0);
    instance.rangeSlider.setHighValue(50.0);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), new QBuilder<>().intNum(propertyName).gte(50).query(new RSQLVisitor()));
    assertEquals(result.get().get(1).query(new RSQLVisitor()), new QBuilder<>().intNum(propertyName).lte(50).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }
}
