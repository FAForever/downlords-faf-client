package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.query.SearchablePropertyMappings.Property;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.ProgrammingError;
import com.faforever.commons.api.dto.Game;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.collect.ImmutableMap;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class SpecificationControllerTest extends AbstractPlainJavaFxTest {
  private SpecificationController instance;

  @Mock
  private I18n i18n;
  private QBuilder qBuilder;

  @Before
  public void setUp() throws IOException {
    instance = new SpecificationController(i18n);

    loadFxml("theme/vault/search/specification.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });

    qBuilder = new QBuilder();
  }

  @Test
  public void testGetRoot() {
    assertThat(instance.getRoot(), is(instance.specificationRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSomething() {
    try {
      instance.setProperties(ImmutableMap.of(
          "name", new Property("i18n.name", true),
          "startTime", new Property("i18n.startTime", false)
      ));
    } catch (IllegalStateException e) {}
    assertThat(instance.propertyField.getItems(), hasItems("name", "startTime"));
  }

  @Test
  public void dontBuildIfPropertyNameIsNull() {
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void dontBuildIfComparisonOperatorIsNull() {
    try {
      instance.propertyField.setValue("Something");
    } catch (IllegalStateException e) {}
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void dontBuildIfValueIsNull() {
    instance.operationField.setItems(FXCollections.observableArrayList(ComparisonOperator.EQ));
    instance.operationField.getSelectionModel().select(0);
    try {
      instance.propertyField.setValue("Something");
    } catch (IllegalStateException e) {}
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void testStringEquals() {
    testWithParams(ComparisonOperator.EQ, "name", "Test", "name==\"Test\"");
  }

  @Test
  public void testStringNotEquals() {
    testWithParams(ComparisonOperator.NE, "name", "Test", "name!=\"Test\"");
  }

  @Test
  public void testStringIn() {
    testWithParams(ComparisonOperator.IN, "name", "Test", "name=in=(\"Test\")");
  }

  @Test
  public void testStringNin() {
    testWithParams(ComparisonOperator.NIN, "name", "Test", "name=out=(\"Test\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testStringGreaterThan() {
    testWithParams(ComparisonOperator.GT, "name", "Test", null);
  }

  @Test
  public void testEnumEquals() {
    testWithParams(ComparisonOperator.EQ, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition==\"SANDBOX\"");
  }

  @Test
  public void testEnumNotEquals() {
    testWithParams(ComparisonOperator.NE, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition!=\"SANDBOX\"");
  }

  @Test
  public void testEnumIn() {
    testWithParams(ComparisonOperator.IN, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition=in=(\"SANDBOX\")");
  }

  @Test
  public void testEnumNin() {
    testWithParams(ComparisonOperator.NIN, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition=out=(\"SANDBOX\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testEnumGreaterThan() {
    testWithParams(ComparisonOperator.GT, "name", "Test", null);
  }

  @Test
  public void testNumberEquals() {
    testWithParams(ComparisonOperator.EQ, "mapVersion.width", 128, "mapVersion.width==\"128\"");
  }

  @Test
  public void testNumberNotEquals() {
    testWithParams(ComparisonOperator.NE, "mapVersion.width", 128, "mapVersion.width!=\"128\"");
  }

  @Test
  public void testNumberGreaterThan() {
    testWithParams(ComparisonOperator.GT, "mapVersion.width", 128, "mapVersion.width=gt=\"128\"");
  }

  @Test
  public void testNumberGreaterThanEquals() {
    testWithParams(ComparisonOperator.GTE, "mapVersion.width", 128, "mapVersion.width=ge=\"128\"");
  }

  @Test
  public void testNumberLessThan() {
    testWithParams(ComparisonOperator.LT, "mapVersion.width", 128, "mapVersion.width=lt=\"128\"");
  }

  @Test
  public void testNumberLessThanEquals() {
    testWithParams(ComparisonOperator.LTE, "mapVersion.width", 128, "mapVersion.width=le=\"128\"");
  }

  @Test
  public void testNumberIn() {
    testWithParams(ComparisonOperator.IN, "mapVersion.width", 128, "mapVersion.width=in=(\"128\")");
  }

  @Test
  public void testNumberNin() {
    testWithParams(ComparisonOperator.NIN, "mapVersion.width", 128, "mapVersion.width=out=(\"128\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testNumberRegularExpression() {
    testWithParams(ComparisonOperator.RE, "mapVersion.width", 128, null);
  }

  @Test
  public void testBooleanEqualsTrue() {
    testWithParams(ComparisonOperator.EQ, "mapVersion.ranked", true, "mapVersion.ranked==\"true\"");
  }

  @Test
  public void testBooleanNotEqualsTrue() {
    testWithParams(ComparisonOperator.NE, "mapVersion.ranked", true, "mapVersion.ranked==\"false\"");
  }

  @Test
  public void testBooleanEqualsFalse() {
    testWithParams(ComparisonOperator.EQ, "mapVersion.ranked", false, "mapVersion.ranked==\"false\"");
  }

  @Test
  public void testBooleanNotEqualsFalse() {
    testWithParams(ComparisonOperator.NE, "mapVersion.ranked", false, "mapVersion.ranked==\"true\"");
  }

  @Test(expected = ProgrammingError.class)
  public void testBooleanLassThan() {
    testWithParams(ComparisonOperator.LT, "mapVersion.ranked", false, null);
  }

  @Test
  public void testInstantEquals() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.EQ, "startTime", LocalDate.now(), "startTime==\"" + format(now) + "\"");
  }

  @Test
  public void testInstantNotEquals() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.NE, "startTime", LocalDate.now(), "startTime!=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantGreaterThan() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.GT, "startTime", LocalDate.now(), "startTime=gt=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantGreaterThanEquals() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.GTE, "startTime", LocalDate.now(), "startTime=ge=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantLessThan() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.LT, "startTime", LocalDate.now(), "startTime=lt=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantLessThanEquals() {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.LTE, "startTime", LocalDate.now(), "startTime=le=\"" + format(now) + "\"");
  }

  private void testWithParams(ComparisonOperator comparisonOperator, String property, Object value, String expected) {
    instance.setRootType(Game.class);
    instance.operationField.setItems(FXCollections.observableArrayList(comparisonOperator));
    instance.operationField.getSelectionModel().select(0);
    instance.propertyField.setValue(property);
    instance.valueField.setValue(value);

    Optional<Condition> optional = instance.appendTo(qBuilder);
    assertThat(optional.isPresent(), is(true));

    String query = (String) optional.get().query(new RSQLVisitor());

    assertThat(query, is(expected));
  }

  private void testInstantWithParams(ComparisonOperator comparisonOperator, String property, LocalDate value, String expected) {
    instance.setRootType(Game.class);
    instance.operationField.setItems(FXCollections.observableArrayList(comparisonOperator));
    instance.operationField.getSelectionModel().select(0);
    instance.propertyField.setValue(property);
    instance.datePicker.setValue(value);

    Optional<Condition> optional = instance.appendTo(qBuilder);
    assertThat(optional.isPresent(), is(true));

    String query = (String) optional.get().query(new RSQLVisitor());

    assertThat(query, is(expected));
  }

  @NotNull
  private String format(LocalDate now) {
    return now.atStartOfDay(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
  }
}
