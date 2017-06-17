package com.faforever.client.query;

import com.faforever.client.api.dto.Game;
import com.faforever.client.i18n.I18n;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.ProgrammingError;
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
  public void setUp() throws Exception {
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
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.specificationRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSomething() throws Exception {
    instance.setProperties(ImmutableMap.of(
        "name", "i18n.name",
        "startTime", "i18n.startTime"
    ));

    assertThat(instance.propertyField.getItems(), hasItems("name", "startTime"));
  }

  @Test
  public void dontBuildIfPropertyNameIsNull() throws Exception {
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void dontBuildIfComparisonOperatorIsNull() throws Exception {
    instance.propertyField.setValue("Something");
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void dontBuildIfValueIsNull() throws Exception {
    instance.operationField.setItems(FXCollections.observableArrayList(ComparisonOperator.EQ));
    instance.operationField.getSelectionModel().select(0);
    instance.propertyField.setValue("Something");
    assertThat(instance.appendTo(qBuilder), is(Optional.empty()));
  }

  @Test
  public void testStringEquals() throws Exception {
    testWithParams(ComparisonOperator.EQ, "name", "Test", "name==\"Test\"");
  }

  @Test
  public void testStringNotEquals() throws Exception {
    testWithParams(ComparisonOperator.NE, "name", "Test", "name!=\"Test\"");
  }

  @Test
  public void testStringIn() throws Exception {
    testWithParams(ComparisonOperator.IN, "name", "Test", "name=in=(\"Test\")");
  }

  @Test
  public void testStringNin() throws Exception {
    testWithParams(ComparisonOperator.NIN, "name", "Test", "name=out=(\"Test\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testStringGreaterThan() throws Exception {
    testWithParams(ComparisonOperator.GT, "name", "Test", null);
  }

  @Test
  public void testEnumEquals() throws Exception {
    testWithParams(ComparisonOperator.EQ, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition==\"SANDBOX\"");
  }

  @Test
  public void testEnumNotEquals() throws Exception {
    testWithParams(ComparisonOperator.NE, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition!=\"SANDBOX\"");
  }

  @Test
  public void testEnumIn() throws Exception {
    testWithParams(ComparisonOperator.IN, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition=in=(\"SANDBOX\")");
  }

  @Test
  public void testEnumNin() throws Exception {
    testWithParams(ComparisonOperator.NIN, "victoryCondition", VictoryCondition.SANDBOX.name(), "victoryCondition=out=(\"SANDBOX\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testEnumGreaterThan() throws Exception {
    testWithParams(ComparisonOperator.GT, "name", "Test", null);
  }

  @Test
  public void testNumberEquals() throws Exception {
    testWithParams(ComparisonOperator.EQ, "mapVersion.width", 128, "mapVersion.width==\"128\"");
  }

  @Test
  public void testNumberNotEquals() throws Exception {
    testWithParams(ComparisonOperator.NE, "mapVersion.width", 128, "mapVersion.width!=\"128\"");
  }

  @Test
  public void testNumberGreaterThan() throws Exception {
    testWithParams(ComparisonOperator.GT, "mapVersion.width", 128, "mapVersion.width=gt=\"128\"");
  }

  @Test
  public void testNumberGreaterThanEquals() throws Exception {
    testWithParams(ComparisonOperator.GTE, "mapVersion.width", 128, "mapVersion.width=ge=\"128\"");
  }

  @Test
  public void testNumberLessThan() throws Exception {
    testWithParams(ComparisonOperator.LT, "mapVersion.width", 128, "mapVersion.width=lt=\"128\"");
  }

  @Test
  public void testNumberLessThanEquals() throws Exception {
    testWithParams(ComparisonOperator.LTE, "mapVersion.width", 128, "mapVersion.width=le=\"128\"");
  }

  @Test
  public void testNumberIn() throws Exception {
    testWithParams(ComparisonOperator.IN, "mapVersion.width", 128, "mapVersion.width=in=(\"128\")");
  }

  @Test
  public void testNumberNin() throws Exception {
    testWithParams(ComparisonOperator.NIN, "mapVersion.width", 128, "mapVersion.width=out=(\"128\")");
  }

  @Test(expected = ProgrammingError.class)
  public void testNumberRegularExpression() throws Exception {
    testWithParams(ComparisonOperator.RE, "mapVersion.width", 128, null);
  }

  @Test
  public void testBooleanEqualsTrue() throws Exception {
    testWithParams(ComparisonOperator.EQ, "mapVersion.ranked", true, "mapVersion.ranked==\"true\"");
  }

  @Test
  public void testBooleanNotEqualsTrue() throws Exception {
    testWithParams(ComparisonOperator.NE, "mapVersion.ranked", true, "mapVersion.ranked==\"false\"");
  }

  @Test
  public void testBooleanEqualsFalse() throws Exception {
    testWithParams(ComparisonOperator.EQ, "mapVersion.ranked", false, "mapVersion.ranked==\"false\"");
  }

  @Test
  public void testBooleanNotEqualsFalse() throws Exception {
    testWithParams(ComparisonOperator.NE, "mapVersion.ranked", false, "mapVersion.ranked==\"true\"");
  }

  @Test(expected = ProgrammingError.class)
  public void testBooleanLassThan() throws Exception {
    testWithParams(ComparisonOperator.LT, "mapVersion.ranked", false, null);
  }

  @Test
  public void testInstantEquals() throws Exception {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.EQ, "startTime", LocalDate.now(), "startTime==\"" + format(now) + "\"");
  }

  @Test
  public void testInstantNotEquals() throws Exception {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.NE, "startTime", LocalDate.now(), "startTime!=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantGreaterThan() throws Exception {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.GT, "startTime", LocalDate.now(), "startTime=gt=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantGreaterThanEquals() throws Exception {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.GTE, "startTime", LocalDate.now(), "startTime=ge=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantLessThan() throws Exception {
    LocalDate now = LocalDate.now();
    testInstantWithParams(ComparisonOperator.LT, "startTime", LocalDate.now(), "startTime=lt=\"" + format(now) + "\"");
  }

  @Test
  public void testInstantLessThanEquals() throws Exception {
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
