package com.faforever.client.map;

import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.annotation.Resource;
import java.util.function.Consumer;

public class MapTileController {

  @FXML
  Label updatedDateLabel;
  @FXML
  Label downloadsLabel;
  @FXML
  ImageView thumbnailImageView;
  @FXML
  Label nameLabel;
  @FXML
  Label playsLabel;
  @FXML
  Node mapTileRoot;

  @Resource
  MapService mapService;
  @Resource
  TimeService timeService;

  private MapBean map;
  private Consumer<MapBean> onOpenDetailListener;

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

    mapService.getInstalledMaps().addListener((ListChangeListener<MapBean>) change -> {
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
    });
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

  @FXML
  void onShowMapDetail() {
    onOpenDetailListener.accept(map);
  }
}
