package com.faforever.client.game;

import com.faforever.client.coop.CoopService;
import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopScenarioBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.faforever.client.theme.UiService.AEON_STYLE_CLASS;
import static com.faforever.client.theme.UiService.CSS_CLASS_ICON;
import static com.faforever.client.theme.UiService.CYBRAN_STYLE_CLASS;
import static com.faforever.client.theme.UiService.SERAPHIM_STYLE_CLASS;
import static com.faforever.client.theme.UiService.UEF_STYLE_CLASS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CoopMapListController implements Controller<VBox> {

  private final CoopService coopService;
  public VBox root;
  public ListView<CoopScenarioBean> scenarioListView;
  public ListView<CoopMissionBean> missionListView;

  private Consumer<CoopMissionBean> onSelectedMissionClicked;
  private boolean autoSelectFirstScenarioMission = true;

  @Override
  public void initialize() {
    scenarioListView.setCellFactory(param -> new StringListCell<>(CoopScenarioBean::getName, this::getFactionIcon));
    missionListView.setCellFactory(param -> missionCell());

    coopService.getScenarios().thenAccept(scenarios -> JavaFxUtil.runLater(() -> {
      scenarioListView.setItems(FXCollections.observableList(scenarios));
      JavaFxUtil.addListener(scenarioListView.getSelectionModel()
          .selectedItemProperty(), (observable, oldValue, newValue) -> {
        if (newValue != null) {
          missionListView.setItems(newValue.getMaps().sorted(Comparator.comparingInt(CoopMissionBean::getOrder)));
          if (autoSelectFirstScenarioMission) {
            missionListView.getSelectionModel().selectFirst();
          }
        }
      });
      scenarioListView.getSelectionModel().selectFirst();
      missionListView.getSelectionModel().selectFirst();
    }));
  }

  public void setControllerAsPopup() {
    getRoot().getStyleClass().add("missions-popup");
    missionListView.setPrefHeight(200);
  }

  public void setAutoSelectFirstScenarioMission(boolean autoSelectFirstScenarioMission) {
    this.autoSelectFirstScenarioMission = autoSelectFirstScenarioMission;
  }

  private ListCell<CoopMissionBean> missionCell() {
    ListCell<CoopMissionBean> cell = new StringListCell<>(this::concatOrderAndMissionName);
    cell.setOnMouseClicked(event -> {
      if (onSelectedMissionClicked != null && !cell.isEmpty()) {
        onSelectedMissionClicked.accept(cell.getItem());
      }
    });
    return cell;
  }

  private String concatOrderAndMissionName(CoopMissionBean mission) {
    return isSelectedScenarioStandalone() ? mission.getName() : String.format("%d - %s", mission.getOrder(), mission.getName());
  }

  public String getFormattedSelectedMissionName() {
    return String.format("%s - %s", scenarioListView.getSelectionModel()
        .getSelectedItem()
        .getType()
        .name(), getSelectedMission().getName());
  }

  public Region getFactionIcon(CoopScenarioBean scenario) {
    Region factionIcon = new Region();
    ObservableList<String> styleClass = factionIcon.getStyleClass();
    styleClass.add(CSS_CLASS_ICON);
    boolean noIcon = false;
    switch (scenario.getFaction()) {
      case UEF -> styleClass.add(UEF_STYLE_CLASS);
      case CYBRAN -> styleClass.add(CYBRAN_STYLE_CLASS);
      case AEON -> styleClass.add(AEON_STYLE_CLASS);
      case SERAPHIM -> styleClass.add(SERAPHIM_STYLE_CLASS);
      default -> noIcon = true;
    }
    return noIcon ? null : factionIcon;
  }

  private boolean isSelectedScenarioStandalone() {
    List<CoopMissionBean> maps = scenarioListView.getSelectionModel().getSelectedItem().getMaps();
    if (maps.size() <= 1) {
      return false;
    }
    int order = maps.get(0).getOrder();
    return maps.stream().mapToInt(CoopMissionBean::getOrder).allMatch(comparedOrder -> comparedOrder == order);
  }

  public ReadOnlyObjectProperty<CoopMissionBean> selectedMissionProperty() {
    return missionListView.getSelectionModel().selectedItemProperty();
  }

  public CoopMissionBean getSelectedMission() {
    return missionListView.getSelectionModel().getSelectedItem();
  }

  public void setOnSelectedMissionClicked(Consumer<CoopMissionBean> onSelectedMissionClicked) {
    this.onSelectedMissionClicked = onSelectedMissionClicked;
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  public void selectMission(String mapFolderName) {
    if (StringUtils.isNotBlank(mapFolderName)) {
      scenarioListView.getItems()
          .stream()
          .filter(scenario -> scenario.getMaps()
              .stream().anyMatch(mission -> mission.getMapFolderName().equals(mapFolderName)))
          .findFirst()
          .ifPresent(scenario -> {
            scenarioListView.getSelectionModel().select(scenario);
            missionListView.getSelectionModel()
                .select(scenario.getMaps()
                    .stream()
                    .filter(mission -> mission.getMapFolderName().equals(mapFolderName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("No `%s` map folder name in `%s` scenario", mapFolderName, scenario.getName()))));
          });
    }
  }
}
