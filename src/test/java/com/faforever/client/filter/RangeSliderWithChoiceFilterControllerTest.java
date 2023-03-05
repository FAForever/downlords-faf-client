package com.faforever.client.filter;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import javafx.util.StringConverter;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class RangeSliderWithChoiceFilterControllerTest extends UITest {

  private final StringConverter<String> converter = new StringConverter<>() {
    @Override
    public String toString(String object) {
      return object + " item";
    }

    @Override
    public String fromString(String string) {
      return null;
    }
  };

  @Mock
  private I18n i18n;

  @InjectMocks
  private RangeSliderWithChoiceFilterController<String, Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/range_slider_filter.fxml", clazz -> instance, instance);
    runOnFxThreadAndWait(() -> {
      instance.setConverter(converter);
      instance.setItems(List.of("first", "second", "third", "four"));
      instance.setText("text");
      instance.setMinMaxValue(-1000, 1000);
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
    assertEquals("first", instance.valueProperty().getValue().item());
    assertEquals(AbstractRangeSliderFilterController.NO_CHANGE, instance.valueProperty().getValue().range());

    runOnFxThreadAndWait(() -> instance.choiceView.getSelectionModel().select("second item"));

    assertEquals("second item", instance.valueProperty().getValue().item());
    assertEquals(AbstractRangeSliderFilterController.NO_CHANGE, instance.valueProperty().getValue().range());
  }

  @Test
  public void testChangeLowValueAndGetObservableValue() {
    runOnFxThreadAndWait(() -> instance.lowValueTextField.setText("-500"));
    assertEquals("first", instance.valueProperty().getValue().item());
    assertEquals(Range.between(-500, 1000), instance.valueProperty().getValue().range());
  }

  @Test
  public void testChangeHighValueAndGetObservableValue() {
    runOnFxThreadAndWait(() -> instance.highValueTextField.setText("500"));
    assertEquals("first", instance.valueProperty().getValue().item());
    assertEquals(Range.between(-1000, 500), instance.valueProperty().getValue().range());
  }

  @Test
  public void testChangeValuesAndGetObservableValue() {
    runOnFxThreadAndWait(() -> {
      instance.lowValueTextField.setText("-500");
      instance.highValueTextField.setText("500");
    });
    assertEquals("first", instance.valueProperty().getValue().item());
    assertEquals(Range.between(-500, 500), instance.valueProperty().getValue().range());
  }

  @Test
  public void testResetFilter() {
    runOnFxThreadAndWait(() -> {
      instance.lowValueTextField.setText("-500");
      instance.highValueTextField.setText("500");
      instance.resetFilter();
    });
    assertEquals("first", instance.choiceView.getSelectionModel().getSelectedItem());
    assertEquals(AbstractRangeSliderFilterController.NO_CHANGE, instance.valueProperty().getValue().range());
  }

  @Test
  public void testSetTex() {
    verify(i18n).get("filter.range", "first item", 0, 0);
  }
}