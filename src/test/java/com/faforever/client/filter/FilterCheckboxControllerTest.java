package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterCheckboxControllerTest extends UITest {

  private FilterCheckboxController<Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new FilterCheckboxController<>();
    loadFxml("theme/filter/checkbox_filter.fxml", clazz -> instance);
  }

  @Test
  public void testSetText() {
    runOnFxThreadAndWait(() -> instance.setText("text"));
    assertEquals("text", instance.getRoot().getText());
  }

  @Test
  public void testHasDefaultValue() {
    runOnFxThreadAndWait(() -> instance.getRoot().fire());
    assertFalse(instance.hasDefaultValue());

    runOnFxThreadAndWait(() -> instance.getRoot().fire());
    assertTrue(instance.hasDefaultValue());
  }

  @Test
  public void testResetFilter() {
    runOnFxThreadAndWait(() -> instance.getRoot().fire());
    assertTrue(instance.getRoot().isSelected());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertFalse(instance.getRoot().isSelected());
  }

  @Test
  public void testRegisterListener() {
    FilteredList<Object> list = new FilteredList<>(FXCollections.observableArrayList(new Object()));
    JavaFxUtil.addListener(instance.predicateProperty(), observable -> list.setPredicate(instance.getPredicate()));
    AtomicBoolean atomicBoolean = new AtomicBoolean();

    instance.registerListener((selected, item) -> atomicBoolean.getAndSet(selected));

    assertFalse(atomicBoolean.get());
    runOnFxThreadAndWait(() -> instance.getRoot().fire());
    assertTrue(atomicBoolean.get());
  }

  @Test
  public void testGetObservable() {
    assertEquals(instance.getRoot().selectedProperty(), instance.valueProperty());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}