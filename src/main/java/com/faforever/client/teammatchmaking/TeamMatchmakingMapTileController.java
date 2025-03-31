package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.commons.lobby.VetoData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing the UI representation of a map tile in the Team Matchmaking feature.
 * Displays map details such as thumbnail, name, author, size, and veto tokens.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TeamMatchmakingMapTileController extends NodeController<Pane> {

  private final MapService mapService;
  private final I18n i18n;
  private final ImageViewHelper imageViewHelper;
  private final MapGeneratorService mapGeneratorService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final MatchmakerPrefs matchmakerPrefs;

  public Pane root;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Label sizeLabel;
  public VBox authorBox;
  public HBox vetoesBox;

  protected final ObjectProperty<MapPoolAssignment> assignment = new SimpleObjectProperty<>();
  @Setter
  private ObservableValue<Integer> vetoTokensLeft;
  private final SimpleIntegerProperty vetoTokensMax = new SimpleIntegerProperty(0);

  @Override
  public Pane getRoot() {
    return root;
  }

  public void setMapAssignment(MapPoolAssignment mapVersion) {
    this.assignment.set(mapVersion);
  }

  public void setVetoTokensMax(int vetoTokensMax) {
    this.vetoTokensMax.set(vetoTokensMax);
  }

  @Override
  protected void onInitialize() {
    ObservableValue<Map> mapObservable = assignment.map(assignment -> assignment.mapVersion().map());

    thumbnailImageView.imageProperty()
                      .bind(assignment.map(assignmentBean -> mapService.loadPreview(assignmentBean.mapVersion(), PreviewSize.SMALL))
                                      .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable));

    // Bind map name
    nameLabel.textProperty().bind(mapObservable.map(map -> {
      String name = map.displayName();
      if (mapGeneratorService.isGeneratedMap(name)) {
        return "map generator";
      }
      return name;
    }));

    // Bind author visibility and text
    authorBox.visibleProperty().bind(mapObservable.map(
        map -> (map.author() != null) || mapGeneratorService.isGeneratedMap(map.displayName())));
    authorLabel.textProperty().bind(mapObservable.map(map -> {
      if (map.author() != null) {
        return map.author().getUsername();
      } else if (mapGeneratorService.isGeneratedMap(map.displayName())) {
        return "Neroxis";
      } else {
        return i18n.get("map.unknownAuthor");
      }
    }));

    sizeLabel.textProperty().bind(assignment.map(MapPoolAssignment::mapVersion)
                                            .map(MapVersion::size)
                                            .map(size -> i18n.get("mapPreview.size", size.widthInKm(), size.heightInKm())));

    // Subscribe to veto token updates
    vetoTokensMax.subscribe(this::updateVetoes);
    matchmakerPrefs.getAppliedVetoes().subscribe(this::updateVetoes);
  }

  private void updateVetoes() {
    int maxTokens = vetoTokensMax.getValue();
    int usedTokens = matchmakerPrefs.getAppliedVetoes().stream()
                                    .filter(veto -> veto.getMapPoolMapVersionId() == assignment.getValue().id())
                                    .findFirst()
                                    .map(VetoData::getVetoTokensApplied)
                                    .orElse(0);

    List<SVGPath> tokens = new ArrayList<>();
    for (int i = 0; i < maxTokens; i++) {
      SVGPath token = new SVGPath();
      token.setContent("M18.148 12.48l5.665-5.66c1.563-1.56 1.563-4.1 0-5.66-1.565-1.57-4.101-1.57-5.665 0l-5.664 5.66L6.82 1.16c-1.563-1.57-4.099-1.57-5.664 0-1.563 1.56-1.563 4.1 0 5.66l5.664 5.66-5.664 5.67c-1.563 1.56-1.563 4.1 0 5.66 1.565 1.57 4.101 1.57 5.664 0l5.664-5.66 5.664 5.66c1.564 1.57 4.1 1.57 5.665 0 1.563-1.56 1.563-4.1 0-5.66l-5.665-5.67");
      token.setStyle("-fx-cursor: hand;");

      int index = i;
      if (i < usedTokens) {
        token.setFill(Paint.valueOf("#ff0000")); // Red for used tokens
      } else {
        index++;
        token.setFill(Paint.valueOf("#000000")); // Black for unused tokens
        token.getStyleClass().add("token-not-activated");
      }

      int finalIndex = index;
      token.setOnMouseClicked(event -> {
        int current = matchmakerPrefs.getAppliedVetoes().stream()
                                     .filter(veto -> veto.getMapPoolMapVersionId() == assignment.getValue().id())
                                     .findFirst()
                                     .map(VetoData::getVetoTokensApplied)
                                     .orElse(0);
        int delta = finalIndex - current;
        if (delta > vetoTokensLeft.getValue()) {
          delta = vetoTokensLeft.getValue();
        }
        int newValue = Math.max(0, current + delta);
        matchmakerPrefs.setVetoData(new VetoData(assignment.getValue().id(), newValue, assignment.getValue().mapPool().mapPool().id()));
      });
      tokens.add(token);
    }

    fxApplicationThreadExecutor.execute(() -> vetoesBox.getChildren().setAll(tokens));
  }
}