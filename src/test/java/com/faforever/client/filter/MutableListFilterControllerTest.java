package com.faforever.client.filter;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class MutableListFilterControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @InjectMocks
  private MutableListFilterController<Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/mutable_list_filter.fxml", clazz -> instance);
  }

  @Test
  public void testVisibleList() {
    assertFalse(instance.listView.isVisible());
    addItemToList("text");
    assertTrue(instance.listView.isVisible());
  }

  @Test
  public void testResetFilter() {
    addItemToList("text");
    assertFalse(instance.listView.getItems().isEmpty());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertTrue(instance.listView.getItems().isEmpty());
  }

  @Test
  public void testResetFilterWhenBounded() {
    runOnFxThreadAndWait(() -> instance.bindBidirectional(new SimpleListProperty<>(FXCollections.observableArrayList("1", "2"))));
    assertFalse(instance.listView.getItems().isEmpty());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertFalse(instance.listView.getItems().isEmpty());
  }

  @Test
  public void testBindBidirectional() {
    ObservableList<String> list = FXCollections.observableArrayList("1", "2");
    SimpleListProperty<String> property = new SimpleListProperty<>(list);
    runOnFxThreadAndWait(() -> instance.bindBidirectional(property));
    assertLinesMatch(List.of("1", "2"), instance.listView.getItems());

    runOnFxThreadAndWait(() -> addItemToList("3"));
    assertLinesMatch(instance.listView.getItems(), property.getValue());
  }

  @Test
  public void testBindBidirectionalAndThrowException() {
    assertThrows(IllegalArgumentException.class, () -> instance.bindBidirectional(new SimpleDoubleProperty()));
  }

  @Test
  public void testHasDefaultValue() {
    assertTrue(instance.hasDefaultValue());
    addItemToList("text");
    assertFalse(instance.hasDefaultValue());
  }

  @Test
  public void testHasDefaultValueWhenBounded() {
    runOnFxThreadAndWait(() -> instance.bindBidirectional(new SimpleListProperty<>(FXCollections.observableArrayList())));
    assertTrue(instance.hasDefaultValue());
  }

  @Test
  public void testSetText() {
    when(i18n.get(any(String.class), any(), any())).thenReturn("text");
    runOnFxThreadAndWait(() -> instance.setText("text"));
    assertEquals("text", instance.getRoot().getText());
  }

  @Test
  public void testSetPromptText() {
    runOnFxThreadAndWait(() -> instance.setPromptText("text"));
    assertEquals("text", instance.addItemTextField.getPromptText());
  }

  @Test
  public void testGetObservable() {
    assertInstanceOf(ObjectBinding.class, instance.getObservable());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  private void addItemToList(String item) {
    runOnFxThreadAndWait(() -> {
      instance.addItemTextField.setText(item);
      instance.addItemTextField.fireEvent(new ActionEvent());
    });
  }
}