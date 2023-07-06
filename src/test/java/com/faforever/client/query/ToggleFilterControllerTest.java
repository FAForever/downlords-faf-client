package com.faforever.client.query;

import com.faforever.client.test.PlatformTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class ToggleFilterControllerTest extends PlatformTest {

  private final String propertyName = "testProp";
  private final String value = "test";

  @InjectMocks
  private ToggleFilterController instance;
  @Mock
  private InvalidationListener queryListener;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/vault/search/toggleFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
    instance.setValue(value);
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.checkBox.setSelected(true);

    verify(queryListener).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.checkBox.setSelected(true);
    instance.clear();

    assertFalse(instance.checkBox.isSelected());
  }

  @Test
  public void testSetTitle() throws Exception {
    instance.setTitle("Test");

    assertEquals(instance.title.getText(), "Test:");
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof GridPane);
  }

  @Test
  public void testGetConditionNotChecked() throws Exception {
    assertTrue(instance.getCondition().isEmpty());
  }

  @Test
  public void testGetConditionChecked() throws Exception {
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.checkBox.setSelected(true);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.eq(value).query(new RSQLVisitor()));
  }
}
