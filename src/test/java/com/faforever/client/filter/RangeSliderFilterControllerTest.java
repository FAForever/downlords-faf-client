package com.faforever.client.filter;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class RangeSliderFilterControllerTest extends UITest {

  @Mock
  private I18n i18n;

  @InjectMocks
  private RangeSliderFilterController<Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/range_slider_filter.fxml", clazz -> instance, instance);
    runOnFxThreadAndWait(() -> {
      instance.setText("text");
      instance.setMinValue(-1000);
      instance.setMaxValue(1000);
      instance.registerListener(mock(BiFunction.class));
    });
  }

  @Test
  public void testTextFieldsAndRangeSliderAreBound() {
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("-500"));
    assertEquals(-500, instance.rangeSlider.getLowValue());

    runOnFxThreadAndWait(() -> instance.highValueTextField.setText("500"));
    assertEquals(500, instance.rangeSlider.getHighValue());

    runOnFxThreadAndWait(() -> instance.rangeSlider.setLowValue(-750));
    assertEquals("-750", instance.lowValueTextField.getText());

    runOnFxThreadAndWait(() -> instance.rangeSlider.setHighValue(750));
    assertEquals("750", instance.highValueTextField.getText());

    runOnFxThreadAndWait(() -> instance.rangeSlider.setLowValue(-1000));
    assertEquals("", instance.lowValueTextField.getText());

    runOnFxThreadAndWait(() -> instance.rangeSlider.setHighValue(1000));
    assertEquals("", instance.highValueTextField.getText());
  }

  @Test
  public void testGetObservableValueWhenNoChange() {
    assertEquals(AbstractRangeSliderFilterController.NO_CHANGE, instance.getObservable().getValue());
  }

  @Test
  public void testChangeLowValueAndGetObservableValue() {
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("-500"));
    assertEquals(Range.between(-500, 1000), instance.getObservable().getValue());
  }

  @Test
  public void testChangeHighValueAndGetObservableValue() {
    runOnFxThreadAndWait(() -> instance.highValueTextField.setText("500"));
    assertEquals(Range.between(-1000, 500), instance.getObservable().getValue());
  }

  @Test
  public void testChangeValuesAndGetObservableValue() {
    runOnFxThreadAndWait(() -> {
      instance.lowValueTextField.setText("-500");
      instance.highValueTextField.setText("500");
    });
    assertEquals(Range.between(-500, 500), instance.getObservable().getValue());
  }

  @Test
  public void testResetFilter() {
    runOnFxThreadAndWait(() -> {
      instance.lowValueTextField.setText("-500");
      instance.highValueTextField.setText("500");
      instance.resetFilter();
    });
    assertEquals(AbstractRangeSliderFilterController.NO_CHANGE, instance.getObservable().getValue());
  }

  @Test
  public void testSetTex() {
    verify(i18n).get("filter.range", "text", 0, 0);
  }

  @Test
  public void testHasLowDefaultValue() {
    assertTrue(instance.hasDefaultLowValue());
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("-500"));
    assertFalse(instance.hasDefaultLowValue());
  }

  @Test
  public void testHasHighDefaultValue() {
    assertTrue(instance.hasDefaultHighValue());
    runOnFxThreadAndWait(() -> instance.highValueTextField.setText("500"));
    assertFalse(instance.hasDefaultHighValue());
  }

  @Test
  public void testHasDefaultValue() {
    assertTrue(instance.hasDefaultValue());
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("-500"));
    assertFalse(instance.hasDefaultValue());
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("500"));
    assertFalse(instance.hasDefaultValue());
    runOnFxThreadAndWait(() -> {
      instance.lowValueTextField.setText("-1000");
      instance.highValueTextField.setText("1000");
    });
    assertTrue(instance.hasDefaultValue());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}