package com.faforever.client.map;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.StarsController;
import com.jfoenix.controls.JFXRippler;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapCardController implements Controller<Node> {

  private final MapService mapService;
  private final I18n i18n;

  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Node mapTileRoot;
  public Label authorLabel;
  public StarsController starsController;
  public Label numberOfReviewsLabel;
  public Label numberOfPlaysLabel;
  public Label sizeLabel;
  public Label maxPlayersLabel;

  private MapBean map;
  private Consumer<MapBean> onOpenDetailListener;
  private ListChangeListener<MapBean> installedMapsChangeListener;
  private InvalidationListener reviewsChangedListener;
  private JFXRippler jfxRippler;

  @Inject
  public MapCardController(MapService mapService, I18n i18n) {
    this.mapService = mapService;
    this.i18n = i18n;
    reviewsChangedListener = observable -> populateReviews();
  }

  public void initialize() {
    jfxRippler = new JFXRippler(mapTileRoot);
    installedMapsChangeListener = change -> {
      while (change.next()) {
        for (MapBean mapBean : change.getAddedSubList()) {
          if (map.getId().equals(mapBean.getId())) {
            setInstalled(true);
            return;
          }
        }
        for (MapBean mapBean : change.getRemoved()) {
          if (map.getId().equals(mapBean.getId())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
  }

  public void setMap(MapBean map) {
    this.map = map;
    Image image;
    if (map.getSmallThumbnailUrl() != null) {
      image = mapService.loadPreview(map, PreviewSize.SMALL);
    } else {
      image = IdenticonUtil.createIdenticon(map.getId());
    }
    thumbnailImageView.setImage(image);
    nameLabel.setText(map.getDisplayName());
    authorLabel.setText(Optional.ofNullable(map.getAuthor()).orElse(i18n.get("map.unknownAuthor")));
    numberOfPlaysLabel.setText(i18n.number(map.getNumberOfPlays()));

    MapSize size = map.getSize();
    sizeLabel.setText(i18n.get("mapPreview.size", size.getWidthInKm(), size.getHeightInKm()));
    maxPlayersLabel.setText(i18n.number(map.getPlayers()));

    ObservableList<MapBean> installedMaps = mapService.getInstalledMaps();
    JavaFxUtil.addListener(installedMaps, new WeakListChangeListener<>(installedMapsChangeListener));

    ObservableList<Review> reviews = map.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  private void populateReviews() {
    ObservableList<Review> reviews = map.getReviews();
    Platform.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(reviews.size()));
      starsController.setValue((float) reviews.stream().mapToInt(Review::getScore).average().orElse(0d));
    });
  }

  private void setInstalled(boolean installed) {
    // FIXME implement
  }

  public Node getRoot() {
    return jfxRippler;
  }

  public void setOnOpenDetailListener(Consumer<MapBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(map);
  }
}
