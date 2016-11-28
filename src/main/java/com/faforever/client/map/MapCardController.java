package com.faforever.client.map;

import com.faforever.client.fx.Controller;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
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
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapCardController implements Controller<Node> {

  private final MapService mapService;
  private final TimeService timeService;
  public Label updatedDateLabel;
  public Label downloadsLabel;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label playsLabel;
  public Node mapTileRoot;
  public Label authorLabel;
  private MapBean map;
  private Consumer<MapBean> onOpenDetailListener;
  private ListChangeListener<MapBean> installedMapsChangeListener;

  @Inject
  public MapCardController(MapService mapService, TimeService timeService) {
    this.mapService = mapService;
    this.timeService = timeService;
  }

  public void initialize() {
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
    playsLabel.setText(String.format("%d", map.getPlays()));
    downloadsLabel.setText(String.format("%d", map.getDownloads()));
    updatedDateLabel.setText(timeService.asDate(map.getCreateTime()));
    authorLabel.setText(map.getAuthor());

    ObservableList<MapBean> installedMaps = mapService.getInstalledMaps();
    synchronized (installedMaps) {
      installedMaps.addListener(new WeakListChangeListener<>(installedMapsChangeListener));
    }
  }

  private void setInstalled(boolean installed) {
    // FIXME implement
  }

  public Node getRoot() {
    return mapTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<MapBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(map);
  }
}
