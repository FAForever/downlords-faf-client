package com.faforever.client.game;

import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGenerationResult;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class MapSelectionController extends NodeController<Pane> {

  private static final int PREVIEW_WIDTH = 200;
  private static final int PREVIEW_HEIGHT = 200;

  private final I18n i18n;
  private final MapService mapService;

  public Pane selectionRoot;
  public GridPane mapsGrid;
  public Label statusLabel;
  public Button okButton;

  private final ObjectProperty<MapGenerationResult> selectedResult = new SimpleObjectProperty<>();
  @Getter
  private final ObservableList<MapGenerationResult> mapResults = FXCollections.observableArrayList();
  @Setter
  private Runnable onOkButtonClickedListener = () -> {};
  @Setter
  private Runnable onCancelButtonClickedListener = () -> {};

  private final ObservableMap<MapGenerationResult, VBox> mapCards = FXCollections.observableHashMap();

  @Override
  protected void onInitialize() {
    mapResults.subscribe(this::updateMapDisplay);
    okButton.disableProperty().bind(selectedResult.isNull());
  }

  public void setMapResults(Collection<MapGenerationResult> results) {
    setSelectedResult(null);
    mapResults.setAll(results);
    log.info("Setting {} map results for selection", results.size());
  }

  private void updateMapCardSelection(VBox card, MapGenerationResult result) {
    if (Objects.equals(selectedResult.get(), result)) {
      card.getStyleClass().add("selected");
    } else {
      card.getStyleClass().remove("selected");
    }
  }

  private void updateMapDisplay() {
    mapsGrid.getChildren().clear();
    mapsGrid.getRowConstraints().clear();

    if (mapResults.isEmpty()) {
      Label noMapsLabel = new Label(i18n.get("game.generateMap.selection.noMaps"));
      noMapsLabel.getStyleClass().add("no-maps-label");
      mapsGrid.add(noMapsLabel, 0, 0);
      return;
    }

    int rowIndex = 0;
    int columnIndex = 0;
    int maxColumns = 3;

    for (MapGenerationResult result : mapResults) {
      VBox mapCard = mapCards.computeIfAbsent(result, this::createMapCard);

      if (columnIndex >= maxColumns) {
        columnIndex = 0;
        rowIndex++;
      }

      mapsGrid.add(mapCard, columnIndex, rowIndex);
      columnIndex++;

      mapCard.setOnMouseClicked(event -> {
        setSelectedResult(result);
      });
    }

    statusLabel.setText(i18n.get("game.generateMap.selection.description"));
  }

  private VBox createMapCard(MapGenerationResult result) {
    VBox card = new VBox(8);
    card.getStyleClass().add("map-card");

    ImageView previewImageView = new ImageView();
    previewImageView.setFitWidth(PREVIEW_WIDTH);
    previewImageView.setFitHeight(PREVIEW_HEIGHT);
    previewImageView.setPreserveRatio(true);
    previewImageView.setSmooth(true);

    String mapName = result.mapName();
    Image previewImage = mapService.loadPreview(mapName, PreviewSize.LARGE);
    previewImageView.setImage(previewImage);

    previewImageView.setOnMouseClicked(event -> onMapPreviewImageClicked(event, mapName));

    card.getChildren().add(previewImageView);

    Label mapNameLabel = new Label(result.mapName());
    mapNameLabel.getStyleClass().add("map-card-label");
    mapNameLabel.setWrapText(true);
    mapNameLabel.setMaxWidth(PREVIEW_WIDTH);

    card.getChildren().addAll(mapNameLabel);

    Tooltip tooltip = new Tooltip(createTooltipText(result));
    Tooltip.install(card, tooltip);

    return card;
  }

  public void onMapPreviewImageClicked(MouseEvent event, String mapName) {
    if (event.getButton() == MouseButton.PRIMARY) {
      PopupUtil.showImagePopup(mapService.loadPreview(mapName, PreviewSize.LARGE));
    }
  }

  private String createTooltipText(MapGenerationResult result) {
    StringBuilder sb = new StringBuilder();
    sb.append(i18n.get("mapSelection.tooltip.map", result.mapName())).append("\n");
    String seed = result.generatorOptions().seed();
    if (StringUtils.isNotEmpty(seed)) {
      sb.append(i18n.get("mapSelection.tooltip.seed", result.generatorOptions().seed())).append("\n");
    }
    sb.append(i18n.get("mapSelection.tooltip.teams", result.generatorOptions().numTeams())).append("\n");
    sb.append(i18n.get("mapSelection.tooltip.spawnCount", result.generatorOptions().spawnCount()));

    if (result.errorMessage().isPresent()) {
      sb.append("\n\n").append(i18n.get("mapSelection.tooltip.error", result.errorMessage().get()));
    }

    return sb.toString();
  }

  public void onCancelButtonClicked() {
    onCancelButtonClickedListener.run();
  }

  public void onOkButtonClicked() {
    onOkButtonClickedListener.run();
  }

  public MapGenerationResult getSelectedResult() {
    return selectedResult.get();
  }

  @Override
  public Pane getRoot() {
    return selectionRoot;
  }

  @VisibleForTesting
  void setSelectedResult(MapGenerationResult result) {
    MapGenerationResult previousResult = selectedResult.get();
    selectedResult.set(result);

    if (result != null) {
      VBox card = mapCards.get(result);
      if (card != null) {
        updateMapCardSelection(card, result);
      }
    }

    if (previousResult != null && previousResult != result) {
      VBox previousCard = mapCards.get(previousResult);
      if (previousCard != null) {
        updateMapCardSelection(previousCard, previousResult);
      }
    }
  }
}
