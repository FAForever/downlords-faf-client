package com.faforever.client.query;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.layout.VBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TextFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private TextFilterController instance;
  @Mock
  private InvalidationListener queryListener;

  @Before
  public void setUp() throws Exception {
    instance = new TextFilterController();

    loadFxml("theme/vault/search/textFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.textField.setText("test");

    verify(queryListener, times(2)).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.textField.setText("test");
    instance.clear();

    assertTrue(instance.textField.getText().isEmpty());
  }

  @Test
  public void testSetTitle() throws Exception {
    instance.setTitle("Test");

    assertEquals(instance.textField.getPromptText(), "Test");
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof VBox);
  }

  @Test
  public void testGetConditionNoText() throws Exception {
    assertTrue(instance.getCondition().isEmpty());
    assertFalse(instance.textField.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionChecked() throws Exception {
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.textField.setText("test");

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.eq("*test*").query(new RSQLVisitor()));
  }
}
