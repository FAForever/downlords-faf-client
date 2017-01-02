package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.query.LogicalNodeController.LogicalOperator;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogicalNodeControllerTest extends AbstractPlainJavaFxTest {
  private LogicalNodeController instance;
  private QBuilder qBuilder;

  @Mock
  private I18n i18n;
  @Mock
  private SpecificationController specificationController;

  @Before
  public void setUp() throws Exception {
    instance = new LogicalNodeController(i18n);

    loadFxml("theme/query/logical_node.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      } else if (clazz == SpecificationController.class) {
        return specificationController;
      }
      return mock(clazz);
    });

    qBuilder = new QBuilder();
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.logicalNodeRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testAppendAnd() throws Exception {
    testWithParams(LogicalOperator.AND, "name", "Test", "test");
  }

  @Test
  public void testAppendOr() throws Exception {
    testWithParams(LogicalOperator.OR, "name", "Test", "test");
  }

  private void testWithParams(LogicalNodeController.LogicalOperator operator, String property, Object value, String expected) {
    instance.logicalOperatorField.setValue(operator);

    Condition condition = qBuilder.string(property).exists();
    when(specificationController.appendTo(any())).thenReturn(Optional.ofNullable(condition));

    Optional<Condition> optional = instance.appendTo(condition);
    assertThat(optional.isPresent(), is(true));
    assertThat(optional.get(), is(condition));
  }
}
