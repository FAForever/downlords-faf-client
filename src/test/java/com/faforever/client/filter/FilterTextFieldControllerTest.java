package com.faforever.client.filter;

import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterTextFieldControllerTest extends PlatformTest {

  private FilterTextFieldController<Object> instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new FilterTextFieldController<>();
    loadFxml("theme/filter/textfield_filter.fxml", clazz -> instance);
  }

  @Test
  public void testSetPromptText() {
    runOnFxThreadAndWait(() -> instance.setPromptText("text"));
    assertEquals("text", instance.getRoot().getPromptText());
  }

  @Test
  public void testResetFilter() {
    runOnFxThreadAndWait(() -> instance.getRoot().setText("text"));
    assertFalse(instance.getRoot().getText().isEmpty());
    runOnFxThreadAndWait(() -> instance.resetFilter());
    assertTrue(instance.getRoot().getText().isEmpty());
  }

  @Test
  public void testHasDefaultValue() {
    assertTrue(instance.hasDefaultValue());
    runOnFxThreadAndWait(() -> instance.getRoot().setText("text"));
    assertFalse(instance.hasDefaultValue());
  }

  @Test
  public void testGetObservable() {
    assertEquals(instance.getRoot().textProperty(), instance.valueProperty());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}