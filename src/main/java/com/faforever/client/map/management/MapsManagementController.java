package com.faforever.client.map.management;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoSelectionModel;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MapsManagementController implements Controller<Node> {

  public GridPane root;
  public ChoiceBox<MapFilter> filterMapsChoiceBox;
  public ListView<MapBean> listView;
  public Button closeButton;

  private final MapService mapService;
  private final UiService uiService;
  private final I18n i18n;

  private final FilteredList<MapBean> maps;
  private Runnable closeButtonAction;

  public MapsManagementController(UiService uiService, MapService mapService, I18n i18n) {
    this.uiService = uiService;
    this.mapService = mapService;
    this.i18n = i18n;
    maps = new FilteredList<>(mapService.getInstalledMaps());
  }

  @Override
  public void initialize() {
    initializeChoiceBox();
    initializeListView();
  }

  private void initializeListView() {
    listView.setCellFactory(param -> uiService.<RemovableMapCellController>loadFxml("theme/vault/map/removable_map_cell.fxml"));
    listView.setSelectionModel(new NoSelectionModel<>());
    listView.setItems(maps);
  }

  private void initializeChoiceBox() {
    filterMapsChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(MapFilter object) {
        return i18n.get(object.getI18n());
      }

      @Override
      public MapFilter fromString(String string) {
        throw new UnsupportedOperationException(); // not required
      }
    });
    filterMapsChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldFilter, newFilter) -> {
      maps.setPredicate(getPredicateBy(newFilter));
      listView.scrollTo(0);
    });
    filterMapsChoiceBox.setItems(FXCollections.observableArrayList(MapFilter.values()));
    filterMapsChoiceBox.setValue(MapFilter.CUSTOM_MAPS);
  }

  private Predicate<MapBean> getPredicateBy(MapFilter filter) {
    return switch (filter) {
      case OFFICIAL_MAPS -> mapService::isOfficialMap;
      case CUSTOM_MAPS -> mapService::isCustomMap;
      case ALL_MAPS -> null;
    };
  }

  public void onCloseButtonClicked() {
    closeButtonAction.run();
  }

  public void setCloseButtonAction(Runnable runnable) {
    this.closeButtonAction = runnable;
  }

  @Override
  public Region getRoot() {
    return root;
  }
}
