package com.faforever.client.vault.search;

import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchControllerTest extends AbstractPlainJavaFxTest {

  private SearchController instance;

  @Mock
  private UiService uiService;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private Consumer<String> searchListener;

  @Before
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/vault/search/logical_node.fxml")).thenAnswer(invocation -> {
      LogicalNodeController controller = mock(LogicalNodeController.class);
      controller.logicalOperatorField = new ChoiceBox<>();
      controller.specificationController = mock(SpecificationController.class);
      controller.specificationController.propertyField = new ComboBox<>();
      controller.specificationController.operationField = new ChoiceBox<>();
      controller.specificationController.valueField = new ComboBox<>();
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });

    instance = new SearchController(uiService);

    loadFxml("theme/vault/search/search.fxml", clazz -> {
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    });
  }

  @Test
  public void testOnSearchButtonClicked() throws Exception {
    instance.setSearchListener(searchListener);
    instance.queryTextField.setText("query");

    instance.onSearchButtonClicked();

    verify(searchListener).accept("query");
  }

  @Test
  public void testBuildQuery() throws Exception {
    instance.onAddCriteriaButtonClicked();

    Condition condition = mock(Condition.class);
    when(specificationController.appendTo(any())).thenReturn(Optional.of(condition));
    when(condition.query(any(RSQLVisitor.class))).thenReturn("name==JUnit");

    specificationController.propertyField.setValue("name");
    specificationController.operationField.getSelectionModel().select(0);
    specificationController.valueField.setValue("JUnit");

    assertThat(instance.queryTextField.getText(), is("name==JUnit"));
  }
}
