package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.fx.NodeController;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.commons.lobby.VetoData;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
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

/**
 * Controller for managing the UI representation of a map tile in the Team Matchmaking feature.
 * Displays map details such as thumbnail, name, author, size, and veto tokens.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TeamMatchmakingMapTileController extends NodeController<Pane> {

  private static final String VETO_ICON_ACTIVE_COLOR = "#FFD700";
  private static final String VETO_ICON_INACTIVE_COLOR = "#FFFFFF";

  private final MapService mapService;
  private final I18n i18n;
  private final ImageViewHelper imageViewHelper;
  private final MapGeneratorService mapGeneratorService;
  private final MatchmakerPrefs matchmakerPrefs;

  public Pane root;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Label sizeLabel;
  public VBox authorBox;
  public HBox vetoesBox;
  public Button minusButton;
  public Button vetoButton;
  public Label tokenCounterLabel;
  public SVGPath vetoSvg;

  protected final ObjectProperty<MapPoolAssignment> assignment = new SimpleObjectProperty<>();
  @Setter
  private ObservableValue<Integer> vetoTokensLeft;
  @Setter
  private SimpleBooleanProperty vetoModeEnabled;
  private final SimpleIntegerProperty vetoTokensMax = new SimpleIntegerProperty(0);
  private final SimpleIntegerProperty maxPerMap = new SimpleIntegerProperty(0);
  private final SimpleIntegerProperty tokenCount = new SimpleIntegerProperty(0);

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

  public void setMaxPerMap(int maxPerMap) {
    this.maxPerMap.set(maxPerMap);
  }

  public void setVetoIconPath(String vetoIconPath) { this.vetoSvg.setContent(vetoIconPath); }

  public void bindVetoesBoxProperties() {
    vetoesBox.mouseTransparentProperty().bind(vetoModeEnabled.not());
    vetoesBox.visibleProperty().bind(vetoModeEnabled.or(tokenCount.greaterThan(0)));
  }

  @Override
  protected void onInitialize() {
    ObservableValue<Map> mapObservable = assignment.map(assignment -> assignment.mapVersion().map());

    thumbnailImageView.imageProperty()
                      .bind(assignment.map(assignmentBean -> mapService.loadPreview(assignmentBean.mapVersion(), PreviewSize.SMALL))
                                      .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable));


    nameLabel.textProperty().bind(mapObservable.map(map -> {
      String name = map.displayName();
      if (mapGeneratorService.isGeneratedMap(name)) {
        return "map generator";
      }
      return name;
    }));

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

    tokenCounterLabel.textProperty().bind(tokenCount.asString());

    vetoSvg.fillProperty().bind(Bindings.when(tokenCount.greaterThan(0))
                                        .then(Paint.valueOf(VETO_ICON_ACTIVE_COLOR))
                                        .otherwise(Paint.valueOf(VETO_ICON_INACTIVE_COLOR)));

    minusButton.visibleProperty().bind(vetoesBox.hoverProperty().and(tokenCount.greaterThan(0)));
    minusButton.managedProperty().bind(minusButton.visibleProperty());

    vetoButton.setOnAction(event -> {
      if (assignment.getValue() == null) return;
      int currentTokenCount = tokenCount.get();
      int currentMaxPerMap = maxPerMap.get();
      boolean isDynamic = currentMaxPerMap == 0;
      if ((isDynamic || currentTokenCount < currentMaxPerMap) && currentTokenCount < vetoTokensMax.get() && vetoTokensLeft.getValue() > 0) {
        matchmakerPrefs.setVetoData(new VetoData(assignment.getValue().id(), currentTokenCount + 1, assignment.getValue().mapPool().mapPool().id()));
      }
    });

    minusButton.setOnAction(event -> {
      if (assignment.getValue() == null) return;
      int current = tokenCount.get();
      if (current > 0) {
        matchmakerPrefs.setVetoData(new VetoData(assignment.getValue().id(), current - 1, assignment.getValue().mapPool().mapPool().id()));
      }
    });

    tokenCount.addListener((obs, oldValue, newValue) -> {
      if (newValue.intValue() >= vetoTokensMax.get()) {
        root.getStyleClass().add("banned");
      } else {
        root.getStyleClass().remove("banned");
      }
    });

    vetoTokensMax.subscribe(this::updateVetoes);
    matchmakerPrefs.getAppliedVetoes().subscribe(this::updateVetoes);
  }

  private void updateVetoes() {
    if (assignment.getValue() == null) {
      tokenCount.set(0);
      return;
    }
    int usedTokens = matchmakerPrefs.getAppliedVetoes().stream()
                                    .filter(veto -> veto.getMapPoolMapVersionId() == assignment.getValue().id())
                                    .findFirst()
                                    .map(VetoData::getVetoTokensApplied)
                                    .orElse(0);
    tokenCount.set(usedTokens);
  }
}