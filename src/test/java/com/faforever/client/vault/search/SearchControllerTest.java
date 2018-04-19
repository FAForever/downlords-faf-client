package com.faforever.client.vault.search;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
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
  private Consumer<SearchConfig> searchListener;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/vault/search/logical_node.fxml")).thenAnswer(invocation -> {
      LogicalNodeController controller = mock(LogicalNodeController.class);
      controller.logicalOperatorField = new ComboBox<>();
      controller.specificationController = mock(SpecificationController.class);
      controller.specificationController.propertyField = new ComboBox<>();
      controller.specificationController.operationField = new ComboBox<>();
      controller.specificationController.valueField = new ComboBox<>();
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });
    when(preferencesService.getPreferences()).thenReturn(new Preferences());

    instance = new SearchController(uiService, i18n, preferencesService);

    loadFxml("theme/vault/search/search.fxml", clazz -> {
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    });

    instance.setSearchableProperties(SearchableProperties.GAME_PROPERTIES);
    instance.setSortConfig(preferencesService.getPreferences().getVaultPrefs().onlineReplaySortConfigProperty());
  }

  @Test
  public void testOnSearchButtonClicked() throws Exception {
    instance.setSearchListener(searchListener);
    instance.queryTextField.setText("query");

    instance.onSearchButtonClicked();

    verify(searchListener).accept(any(SearchConfig.class));
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

  @Test
  public void testSorting() throws Exception {
    instance.setSearchListener(searchListener);
    instance.queryTextField.setText("query");
    instance.sortOrderChoiceBox.getSelectionModel().select(SortOrder.ASC);
    instance.sortPropertyComboBox.getSelectionModel().select("game.title");

    instance.onSearchButtonClicked();

    SortConfig mapSortConfig = preferencesService.getPreferences().getVaultPrefs().getOnlineReplaySortConfig();
    assertEquals(mapSortConfig.getSortOrder(), SortOrder.ASC);
    verify(searchListener).accept(new SearchConfig(mapSortConfig, "query"));
  }

}
