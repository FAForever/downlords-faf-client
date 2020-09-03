package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.mod.ModVersion.ModType;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModManagerController implements Controller<Parent> {

  private static final Predicate<ModVersion> UI_FILTER = modVersion -> modVersion.getModType() == ModType.UI;
  private static final Predicate<ModVersion> SIM_FILTER = modVersion -> modVersion.getModType() == ModType.SIM;

  private final ModService modService;
  /**
   * Stores what is selected. All UI and SIM mods are in it.
   */
  private final Map<ModVersion, Boolean> modToSelectedMap = new HashMap<>();

  public ToggleButton uiModsButton;
  public ToggleGroup viewToggleGroup;
  public ToggleButton simModsButton;
  public ListView<ModVersion> modListView;
  public VBox root;

  private FilteredList<ModVersion> modVersionFilteredList;

  public void onShowUIMods() {
    filterModList();
  }

  public void onShowSimMods() {
    filterModList();
  }

  public void onDeselectModsButtonClicked() {
    modListView.getSelectionModel().clearSelection();
    modVersionFilteredList.forEach(modVersion -> modToSelectedMap.put(modVersion, false));
  }

  public void onReloadModsButtonClicked() {
    modService.loadInstalledMods();
    loadActivatedMods();
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    modListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    modListView.setCellFactory(modListCellFactory());

    viewToggleGroup.selectToggle(uiModsButton);

    loadActivatedMods();

    modListView.scrollTo(modListView.getSelectionModel().getSelectedItem());
  }

  private void loadActivatedMods() {
    ObservableList<ModVersion> installedModVersions = modService.getInstalledModVersions();
    try {
      List<ModVersion> activatedSimAndUIMods = modService.getActivatedSimAndUIMods();
      installedModVersions.forEach(modVersion -> modToSelectedMap.put(modVersion, activatedSimAndUIMods.contains(modVersion)));
    } catch (IOException e) {
      log.error("Activated mods could not be loaded", e);
    }
    modVersionFilteredList = new FilteredList<>(installedModVersions);
    modVersionFilteredList.setPredicate(viewToggleGroup.getSelectedToggle() == uiModsButton ? UI_FILTER : SIM_FILTER);
    modListView.setItems(modVersionFilteredList);
    filterModList();
  }

  private void filterModList() {
    modVersionFilteredList.setPredicate(viewToggleGroup.getSelectedToggle() == uiModsButton ? UI_FILTER : SIM_FILTER);
    modVersionFilteredList.forEach(modVersion -> {
      if (modToSelectedMap.get(modVersion)) {
        modListView.getSelectionModel().select(modVersion);
      } else {
        modListView.getSelectionModel().clearSelection(modListView.getItems().indexOf(modVersion));
      }
    });
  }

  @NotNull
  private Callback<ListView<ModVersion>, ListCell<ModVersion>> modListCellFactory() {
    return param -> {
      ListCell<ModVersion> cell = new StringListCell<>(ModVersion::getDisplayName);
      cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        modListView.requestFocus();
        MultipleSelectionModel<ModVersion> selectionModel = modListView.getSelectionModel();
        if (!cell.isEmpty()) {
          int index = cell.getIndex();
          if (selectionModel.getSelectedIndices().contains(index)) {
            selectionModel.clearSelection(index);
            modToSelectedMap.put(cell.getItem(), false);
          } else {
            selectionModel.select(index);
            modToSelectedMap.put(cell.getItem(), true);
          }
          event.consume();
        }
      });
      return cell;
    };
  }

  public List<ModVersion> apply() {
    List<ModVersion> mods = modToSelectedMap.entrySet().stream()
        .filter(Entry::getValue)
        .map(Entry::getKey)
        .collect(Collectors.toList());
    try {
      modService.overrideActivatedMods(mods);
    } catch (IOException e) {
      log.warn("Activated mods could not be updated", e);
    }
    return mods;
  }
}
