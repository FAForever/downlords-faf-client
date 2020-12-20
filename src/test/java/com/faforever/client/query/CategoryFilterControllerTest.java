package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.control.MenuButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategoryFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private final List<String> items = Arrays.asList("test1", "test2");
  private final LinkedHashMap<String, String> itemMap = new LinkedHashMap<>();
  private CategoryFilterController instance;
  @Mock
  private InvalidationListener queryListener;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    instance = new CategoryFilterController(i18n);
    itemMap.put("1", items.get(0));
    itemMap.put("2", items.get(1));

    loadFxml("theme/vault/search/categoryFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
    instance.setItems(items);
  }

  @Test
  public void testSetItems() throws Exception {
    instance.setItems(items);

    assertArrayEquals(items.toArray(), instance.checkListView.getItems().toArray());
  }

  @Test
  public void testSetItemMap() throws Exception {
    instance.setItems(itemMap);

    assertArrayEquals(itemMap.keySet().toArray(), instance.checkListView.getItems().toArray());
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.checkListView.getItemBooleanProperty(0).setValue(true);

    verify(queryListener).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));
    instance.clear();

    instance.checkListView.getItems().forEach(item ->
        assertFalse(instance.checkListView.getItemBooleanProperty(item).getValue()));
    assertFalse(instance.checkListView.getItems().isEmpty());
  }

  @Test
  public void testSetTitle() throws Exception {
    when(i18n.get(anyString(), any(), any())).thenReturn("Test");
    instance.setTitle("Test");

    assertTrue(instance.menu.textProperty().isBound());
    assertEquals("Test", instance.menu.getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof MenuButton);
  }

  @Test
  public void testGetConditionNonChecked() throws Exception {
    assertTrue(instance.getCondition().isEmpty());
    assertFalse(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionCheckedNoMap() throws Exception {
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()), property.in(instance.checkListView.getCheckModel().getCheckedItems().toArray()).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionCheckedWithMap() throws Exception {
    instance.setItems(itemMap);
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()),
        property.in(instance.checkListView.getCheckModel().getCheckedItems().stream().map(itemMap::get).toArray()).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }
}
