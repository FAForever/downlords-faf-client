package com.faforever.client.query;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.layout.GridPane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class ToggleFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private final String value = "test";
  private ToggleFilterController instance;
  @Mock
  private InvalidationListener queryListener;

  @Before
  public void setUp() throws Exception {
    instance = new ToggleFilterController();

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
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.in(value).query(new RSQLVisitor()));
  }
}
