package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.geometry.Pos;

import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.NodeController;
import com.faforever.client.map.MapService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor

public class TeamMatchmakingMapTileController extends NodeController<Pane> {

  private final MapService mapService;
  private final I18n i18n;
  private final ImageViewHelper imageViewHelper;
  private final MapGeneratorService mapGeneratorService;
  private double relevanceLevel = 1;
  protected final ObjectProperty<MapVersion> entity = new SimpleObjectProperty<>();
  public Pane root;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Label sizeLabel;
  public VBox authorBox;

  @Override
  public Pane getRoot() {
    return root;
  }


  public void init(MapVersion mapVersion, double relevanceLevel) {
    this.relevanceLevel = relevanceLevel;
    this.entity.set(mapVersion);
  }


  @Override
  protected void onInitialize(){
    thumbnailImageView.imageProperty().bind(entity.map(mapVersionBean -> mapService.loadPreview(mapVersionBean, PreviewSize.SMALL))
                                                  .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable));
    thumbnailImageView.effectProperty().bind(entity.map(mapVersion -> {
      ColorAdjust grayscaleEffect = new ColorAdjust();
      grayscaleEffect.setSaturation(-1 + this.relevanceLevel);
      return grayscaleEffect;
    }));
    ObservableValue<Map> mapObservable = entity.map(MapVersion::map);

    nameLabel.textProperty().bind(mapObservable.map(map -> {
      String name = map.displayName();
      if(mapGeneratorService.isGeneratedMap(name))
        return "map generator";
      return name;
    }));

    authorBox.visibleProperty().bind(mapObservable.map(map -> (map.author() != null) || (mapGeneratorService.isGeneratedMap(map.displayName()))));
    authorLabel.textProperty().bind(mapObservable.map(map -> {
      if (map.author() != null) {
        return map.author().usernameProperty().get();
      } else if (mapGeneratorService.isGeneratedMap(map.displayName())) {
        return "Neroxis";
      } else {
        return "";
      }
    }));
    sizeLabel.textProperty().bind(entity.map(MapVersion::size).map(size -> i18n.get("mapPreview.size", size.widthInKm(), size.heightInKm())));
  }
}