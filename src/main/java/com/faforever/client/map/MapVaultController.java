package com.faforever.client.map;

import com.faforever.client.game.MapBean;
import com.faforever.client.util.JavaFxUtil;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapVaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int MAX_SUGGESTIONS = 10;

  @FXML
  Pane contentPane;
  @FXML
  Pane searchResultGroup;
  @FXML
  Pane searchResultPane;
  @FXML
  Pane showroomGroup;
  @FXML
  Pane loadingPane;
  @FXML
  TextField searchTextField;
  @FXML
  Pane recommendedUiMapsPane;
  @FXML
  Pane newestMapsPane;
  @FXML
  Pane popularMapsPane;
  @FXML
  Pane mostLikedMpas;
  @FXML
  Pane mapVaultRoot;

  @Resource
  MapService mapService;

  private boolean initialized;

  public Node getRoot() {
    return mapVaultRoot;
  }

  @FXML
  void initialize() {
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
  }


  public void setUpIfNecessary() {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMaps();

    JavaFxUtil.makeSuggestionField(searchTextField, this::createMapSuggestions, aVoid -> onSearchMapButtonClicked());
  }

  private void displayShowroomMaps() {
    enterLoadingState();
    mapService.getMostDownloadedMaps(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, popularModsPane))
        .thenCompose(aVoid -> mapService.getMostLikedMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, mostLikedMods))
        .thenCompose(aVoid -> mapService.getNewestMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, newestModsPane))
        .thenCompose(aVoid -> mapService.getMostLikedUiMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, recommendedUiModsPane))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
  }

  @FXML
  void onSearchMapButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();

    mapService.lookupMap(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void enterLoadingState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
  }

  @FXML
  void onResetButtonClicked() {
    searchTextField.clear();
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
  }

  private void enterSearchResultState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
  }

  private CompletableFuture<Set<Label>> createMapSuggestions(String string) {
    return mapService.lookupMap(string, MAX_SUGGESTIONS).thenApply(new Function<List<MapBean>, Set<Label>>() {
      @Override
      public Set<Label> apply(List<MapBean> mapBeans) {
        return mapBeans.stream()
            .map(result -> {
              String name = result.getDisplayName();
              Label item = new Label(name) {
                @Override
                public int hashCode() {
                  return getText().hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                  return obj != null
                      && obj.getClass() == getClass()
                      && getText().equals(((Label) obj).getText());
                }
              };
              item.setUserData(name);
              return item;
            })
            .collect(Collectors.toSet());
      }
    });
  }

  @FXML
  void onSearchMapsButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();

    mapService.lookupMap(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void displaySearchResult(List<MapBean> modInfoBeans) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateMods(modInfoBeans, searchResultPane);
  }

  public void setUpIfNecessary() {
    // FIXME implement
  }
}
