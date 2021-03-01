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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BinaryFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private final String firstValue = "True";
  private final String secondValue = "False";
  private final String firstLabel = "Yes";
  private final String secondLabel = "No";
  private BinaryFilterController instance;
  @Mock
  private InvalidationListener queryListener;

  @Before
  public void setUp() throws Exception {
    instance = new BinaryFilterController();

    loadFxml("theme/vault/search/binaryFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
    instance.setOptions(firstLabel, firstValue, secondLabel, secondValue);
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.firstCheckBox.setSelected(false);
    instance.secondCheckBox.setSelected(false);
    verify(queryListener, times(2)).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.firstCheckBox.setSelected(false);
    instance.secondCheckBox.setSelected(false);
    instance.clear();

    assertTrue(instance.firstCheckBox.isSelected());
    assertTrue(instance.secondCheckBox.isSelected());
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
  public void testGetConditionBothSame() throws Exception {
    assertTrue(instance.getCondition().isEmpty());

    instance.secondCheckBox.setSelected(false);
    instance.firstCheckBox.setSelected(false);

    assertTrue(instance.getCondition().isEmpty());
  }

  @Test
  public void testGetConditionOneChecked() throws Exception {
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.secondCheckBox.setSelected(false);

    assertTrue(instance.getCondition().isPresent());
    assertEquals(instance.getCondition().get().get(0).query(new RSQLVisitor()), property.in(firstValue).query(new RSQLVisitor()));

    property = new QBuilder<>().string(propertyName);
    instance.secondCheckBox.setSelected(true);
    instance.firstCheckBox.setSelected(false);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.in(secondValue).query(new RSQLVisitor()));
  }
}
