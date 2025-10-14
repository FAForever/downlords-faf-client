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
import com.faforever.client.preferences.VetoKey;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
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
import java.util.function.Consumer;

/**
 * Controller for managing the UI representation of a map tile in the Team Matchmaking feature. Displays map details
 * such as thumbnail, name, author, size, and veto tokens.
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
  private ObservableValue<Integer> vetoTokensLeft;
  private BooleanProperty vetoModeEnabled;
  private final IntegerProperty vetoTokensMax = new SimpleIntegerProperty(0);
  private final IntegerProperty maxPerMap = new SimpleIntegerProperty(0);
  private final BooleanBinding isMaxPerMapDynamic = maxPerMap.isEqualTo(0);
  private final IntegerProperty tokenCount = new SimpleIntegerProperty(0);
  @Setter
  private Consumer<MapVersion> onTileClickedListener;

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

  public void setVetoIconPath(String vetoIconPath) {this.vetoSvg.setContent(vetoIconPath);}

  void setVetoTokensLeft(ObservableValue<Integer> vetoTokensLeft) {
    this.vetoTokensLeft = vetoTokensLeft;
  }

  void setVetoModeEnabled(BooleanProperty vetoModeEnabled) {
    this.vetoModeEnabled = vetoModeEnabled;
  }

  public void bindVetoesBoxProperties() {
    vetoesBox.mouseTransparentProperty().bind(vetoModeEnabled.not());
    vetoesBox.visibleProperty().bind(vetoModeEnabled.or(tokenCount.greaterThan(0)));
  }

  @Override
  protected void onInitialize() {
    ObservableValue<Map> mapObservable = assignment.map(assignment -> assignment.mapVersion().map());
    ObservableValue<Boolean> isGeneratedMap = mapObservable.map(map -> mapGeneratorService.isGeneratedMap(map.displayName()));

    thumbnailImageView.imageProperty()
                      .bind(assignment
                                .map(assignmentBean -> mapService.loadPreview(assignmentBean.mapVersion(), PreviewSize.SMALL))
                                .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable));

    nameLabel.textProperty().bind(mapObservable.map(map -> {
      if (isGeneratedMap.getValue()) {
        return "map generator";
      }
      return map.displayName();
    }));

    authorBox.visibleProperty()
             .bind(mapObservable.map(
                 map -> (map.author() != null) || isGeneratedMap.getValue()));

    authorLabel.textProperty().bind(mapObservable.map(map -> {
      if (map.author() != null) {
        return map.author().getUsername();
      } else if (isGeneratedMap.getValue()) {
        return "Neroxis";
      } else {
        return i18n.get("map.unknownAuthor");
      }
    }));

    sizeLabel.textProperty()
             .bind(assignment.map(MapPoolAssignment::mapVersion)
                             .map(MapVersion::size)
                             .map(size -> i18n.get("mapPreview.size", size.widthInKm(), size.heightInKm())));

    tokenCounterLabel.textProperty().bind(tokenCount.asString());

    vetoSvg.fillProperty()
           .bind(Bindings.when(tokenCount.greaterThan(0))
                         .then(Paint.valueOf(VETO_ICON_ACTIVE_COLOR))
                         .otherwise(Paint.valueOf(VETO_ICON_INACTIVE_COLOR)));

    minusButton.visibleProperty().bind(vetoesBox.hoverProperty().and(tokenCount.greaterThan(0)));
    minusButton.managedProperty().bind(minusButton.visibleProperty());

    root.setOnMouseClicked(_ -> {
      if (onTileClickedListener != null && assignment.getValue() != null && !isGeneratedMap.getValue()) {
        onTileClickedListener.accept(assignment.getValue().mapVersion());
      }
    });

    root.cursorProperty()
        .bind(isGeneratedMap.map(gen -> gen ? Cursor.DEFAULT : Cursor.HAND));

    vetoButton.setOnAction(event -> {
      if (assignment.getValue() == null) {
        return;
      }
      int currentTokenCount = tokenCount.get();
      int currentMaxPerMap = maxPerMap.get();
      if ((isMaxPerMapDynamic.get() || currentTokenCount < currentMaxPerMap) && currentTokenCount < vetoTokensMax.get() && vetoTokensLeft.getValue() > 0) {
        matchmakerPrefs.setTokensForMap(
            new VetoKey(assignment.getValue().mapPool().mapPool().id(), assignment.getValue().id()),
            currentTokenCount + 1);
      }
      event.consume();
    });

    minusButton.setOnAction(event -> {
      if (assignment.getValue() == null) {
        return;
      }
      int current = tokenCount.get();
      if (current > 0) {
        matchmakerPrefs.setTokensForMap(
            new VetoKey(assignment.getValue().mapPool().mapPool().id(), assignment.getValue().id()), current - 1);
      }
      event.consume();
    });

    tokenCount.when(showing).subscribe(_ -> updateBannedState());
    maxPerMap.when(showing).subscribe(_ -> updateBannedState());
    updateBannedState();

    vetoTokensMax.when(showing).subscribe(this::updateVetoes);
  }

  @Override
  protected void onShow() {
    addShownSubscription(matchmakerPrefs.getAppliedVetoes().subscribe(this::updateVetoes));
    this.updateVetoes();
  }

  private void updateBannedState() {
    if (!isMaxPerMapDynamic.get() && tokenCount.get() >= maxPerMap.get()) {
      if (!root.getStyleClass().contains("tmm-maplist-tile_banned")) {
        root.getStyleClass().add("tmm-maplist-tile_banned");
      }
    } else {
      root.getStyleClass().remove("tmm-maplist-tile_banned");
    }
  }

  private void updateVetoes() {
    if (assignment.getValue() == null) {
      tokenCount.set(0);
      return;
    }
    VetoKey key = new VetoKey(assignment.getValue().mapPool().mapPool().id(), assignment.getValue().id());
    int usedTokens = matchmakerPrefs.getAppliedVetoes().getOrDefault(key, 0);
    tokenCount.set(usedTokens);
  }
}