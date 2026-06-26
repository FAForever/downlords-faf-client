package com.faforever.client.game;

import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGenerationResult;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
@Getter
@Setter
public class MapSelectionController extends NodeController<Pane> {

  private static final int PREVIEW_WIDTH = 200;
  private static final int PREVIEW_HEIGHT = 200;

  private final I18n i18n;
  private final GeneratorPrefs generatorPrefs;
  private final MapService mapService;

  public Pane selectionRoot;
  public GridPane mapsGrid;
  public Label statusLabel;
  public Button okButton;

  private final ObjectProperty<MapGenerationResult> selectedResult = new SimpleObjectProperty<>();
  private final ObservableList<MapGenerationResult> mapResults = FXCollections.observableArrayList();
  private Runnable onOkButtonClickedListener;
  private Runnable onCancelButtonClickedListener;

  @Override
  protected void onInitialize() {
    mapResults.addListener((ListChangeListener<? super MapGenerationResult>) _ -> updateMapDisplay());

    okButton.disableProperty().bind(selectedResult.isNull());
  }

  public void setMapResults(List<MapGenerationResult> results) {
    selectedResult.set(null);
    mapResults.setAll(results);
    log.info("Setting {} map results for selection", results.size());
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

    AtomicInteger rowIndex = new AtomicInteger(0);
    AtomicInteger columnIndex = new AtomicInteger(0);
    int maxColumns = 3;

    for (MapGenerationResult result : mapResults) {
      VBox mapCard = createMapCard(result);

      if (columnIndex.get() >= maxColumns) {
        columnIndex.set(0);
        rowIndex.incrementAndGet();
      }

      mapsGrid.add(mapCard, columnIndex.get(), rowIndex.get());
      columnIndex.incrementAndGet();

      mapCard.setOnMouseClicked(event -> {
        selectedResult.set(result);
        updateMapDisplay();
      });
    }

    if (columnIndex.get() > 0) {
      rowIndex.incrementAndGet();
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

    SimpleObjectProperty<Image> previewImageProperty = new SimpleObjectProperty<>();
    previewImageView.imageProperty().bind(previewImageProperty);

    String mapName = result.mapName();
    previewImageProperty.set(mapService.loadPreview(mapName, PreviewSize.LARGE));

    previewImageView.setOnMouseClicked(event -> {
      if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
        Image image = previewImageProperty.get();
        if (image != null) {
          PopupUtil.showImagePopup(image);
        }
      }
    });

    card.getChildren().add(previewImageView);

    if (selectedResult.get() == result) {
      card.getStyleClass().add("selected");
    }

    Label mapNameLabel = new Label(result.mapName());
    mapNameLabel.getStyleClass().add("map-card-label");
    mapNameLabel.setWrapText(true);
    mapNameLabel.setMaxWidth(PREVIEW_WIDTH);

    card.getChildren().addAll(mapNameLabel);

    Tooltip tooltip = new Tooltip(createTooltipText(result));
    Tooltip.install(card, tooltip);

    return card;
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

    if (!result.isSuccess() && result.errorMessage().isPresent()) {
      sb.append("\n\n").append(i18n.get("mapSelection.tooltip.error", result.errorMessage().get()));
    }

    return sb.toString();
  }

  public void onCancelButtonClicked() {
    if (onCancelButtonClickedListener != null) {
      onCancelButtonClickedListener.run();
    }
  }

  public void onOkButtonClicked() {
    if (onOkButtonClickedListener != null) {
      onOkButtonClickedListener.run();
    }
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
    selectedResult.set(result);
  }
}
