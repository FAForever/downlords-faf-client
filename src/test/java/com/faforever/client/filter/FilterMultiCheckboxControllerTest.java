package com.faforever.client.filter;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.ListProperty;
import javafx.util.StringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterMultiCheckboxControllerTest extends PlatformTest {

  @Mock
  private I18n i18n;

  @InjectMocks
  private FilterMultiCheckboxController<Integer, Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/multicheckbox_filter.fxml", clazz -> instance);
  }

  @Test
  public void testResetFilterWithSearchBar() {
    List<Integer> items = IntStream.rangeClosed(1, 10).boxed().toList();
    runOnFxThreadAndWait(() -> {
      instance.setItems(items);
      instance.searchTextField.setText("6");
      instance.listView.getCheckModel().check("6");
    });
    assertFalse(instance.getSelectedItems().isEmpty());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertTrue(instance.searchTextField.getText().isEmpty());
    assertTrue(instance.getSelectedItems().isEmpty());
    assertLinesMatch(items.stream().map(Object::toString).collect(Collectors.toList()), instance.listView.getItems());
  }

  @Test
  public void testResetFilter() {
    runOnFxThreadAndWait(() -> {
      instance.setItems(List.of(1, 2));
      instance.listView.getCheckModel().check("1");
    });
    assertFalse(instance.getSelectedItems().isEmpty());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertTrue(instance.getSelectedItems().isEmpty());
    assertLinesMatch(List.of("1", "2"), instance.listView.getItems());
  }

  @Test
  public void testCheckUncheckItemsWithUsingSearchBar() {
    runOnFxThreadAndWait(() -> instance.setItems(IntStream.rangeClosed(1, 10).boxed().toList()));
    runOnFxThreadAndWait(() -> {
      instance.listView.getCheckModel().check("2");
      instance.listView.getCheckModel().check("4");
      instance.searchTextField.setText("8");
      instance.listView.getCheckModel().check("8");
      instance.searchTextField.setText("4");
      instance.listView.getCheckModel().clearCheck("4");
    });
    assertTrue(instance.getSelectedItems().containsAll(List.of(2, 8)));

    runOnFxThreadAndWait(() -> {
      instance.searchTextField.setText("5");
      instance.listView.getCheckModel().check("5");
    });
    assertTrue(instance.getSelectedItems().containsAll(List.of(2, 5, 8)));

    runOnFxThreadAndWait(() -> instance.searchTextField.clear());
    assertTrue(instance.getSelectedItems().containsAll(List.of(2, 5, 8)));

    runOnFxThreadAndWait(() -> instance.listView.getCheckModel().clearCheck("2"));
    assertTrue(instance.getSelectedItems().containsAll(List.of(5, 8)));
  }

  @Test
  public void testCheckUncheckItems() {
    runOnFxThreadAndWait(() -> instance.setItems(List.of(1, 2, 3, 4)));
    runOnFxThreadAndWait(() -> {
      instance.listView.getCheckModel().check("2");
      instance.listView.getCheckModel().check("4");
    });
    assertFalse(instance.getSelectedItems().isEmpty());
    assertEquals(2, instance.getSelectedItems().size());
    assertTrue(instance.getSelectedItems().containsAll(List.of(2, 4)));
  }

  @Test
  public void testSetConverter() {
    instance.setConverter(new StringConverter<>() {
      @Override
      public String toString(Integer object) {
        return object + " item";
      }

      @Override
      public Integer fromString(String string) {
        return null;
      }
    });
    runOnFxThreadAndWait(() -> instance.setItems(List.of(1, 2)));
    assertLinesMatch(List.of("1 item", "2 item"), instance.listView.getItems());
  }

  @Test
  public void testSetText() {
    runOnFxThreadAndWait(() -> instance.setText("text"));
    assertEquals("text", instance.getRoot().getText());
  }

  @Test
  public void testHasDefaultValue() {
    runOnFxThreadAndWait(() -> instance.setItems(List.of(1, 2)));
    assertTrue(instance.hasDefaultValue());
    runOnFxThreadAndWait(() -> instance.listView.getCheckModel().check("1"));
    assertFalse(instance.hasDefaultValue());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  @Test
  public void testGetObservable() {
    assertInstanceOf(ListProperty.class, instance.valueProperty());
  }
}