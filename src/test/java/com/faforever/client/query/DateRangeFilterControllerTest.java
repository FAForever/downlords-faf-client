package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TimeService;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.InstantProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.scene.control.MenuButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DateRangeFilterControllerTest extends AbstractPlainJavaFxTest {

  private final String propertyName = "testProp";
  private final LocalDate before = LocalDate.now();
  private final LocalDate after = LocalDate.EPOCH;
  private DateRangeFilterController instance;
  @Mock
  private I18n i18n;
  @Mock
  private TimeService timeService;
  @Mock
  private InvalidationListener queryListener;

  @Before
  public void setUp() throws Exception {
    instance = new DateRangeFilterController(i18n, timeService);

    loadFxml("theme/vault/search/dateRangeFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
  }

  @Test
  public void testInitialNull() throws Exception {
    assertNull(instance.beforeDate.getValue());
    assertNull(instance.afterDate.getValue());
  }

  @Test
  public void testSetDate() throws Exception {
    instance.addQueryListener(queryListener);
    instance.setBeforeDate(before);
    instance.setAfterDate(after);

    assertEquals(instance.beforeDate.getValue(), before);
    assertEquals(instance.afterDate.getValue(), after);
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.setBeforeDate(before);
    instance.setAfterDate(after);

    verify(queryListener, times(2)).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.setBeforeDate(before);
    instance.setAfterDate(after);
    instance.clear();

    assertNull(instance.beforeDate.getValue());
    assertNull(instance.afterDate.getValue());

    instance.setInitialYearsBefore(1);
    instance.setBeforeDate(before);
    instance.setAfterDate(after);
    instance.clear();

    assertNull(instance.beforeDate.getValue());
    assertEquals(LocalDate.now().minus(1, ChronoUnit.YEARS), instance.afterDate.getValue());
  }

  @Test
  public void testSetTitle() throws Exception {
    when(i18n.get(anyString(), any(), any(), any())).thenReturn("Test");
    instance.setTitle("Test");

    assertTrue(instance.menu.textProperty().isBound());
    assertEquals("Test", instance.menu.getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof MenuButton);
  }

  @Test
  public void testGetConditionAfter() throws Exception {
    InstantProperty property = new QBuilder<>().instant(propertyName);
    instance.setAfterDate(after);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()),
        property.after(after.atStartOfDay().toInstant(ZoneOffset.UTC), false).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionBefore() throws Exception {
    InstantProperty property = new QBuilder<>().instant(propertyName);
    instance.setBeforeDate(before);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(0).query(new RSQLVisitor()),
        property.before(before.atStartOfDay().toInstant(ZoneOffset.UTC), false).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionRange() throws Exception {
    instance.setAfterDate(after);
    instance.setBeforeDate(before);

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().get(1).query(new RSQLVisitor()),
        new QBuilder<>().instant(propertyName).before(before.atStartOfDay().toInstant(ZoneOffset.UTC), false)
            .query(new RSQLVisitor()));
    assertEquals(result.get().get(0).query(new RSQLVisitor()),
        new QBuilder<>().instant(propertyName).after(after.atStartOfDay().toInstant(ZoneOffset.UTC), false)
            .query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }
}
