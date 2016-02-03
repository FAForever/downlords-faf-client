package com.faforever.client.map;

import com.faforever.client.game.MapBean;
import com.faforever.client.i18n.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

public class MapDetailController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  ImageView largeImagePreview;
  @FXML
  Label mapNameLabel;
  @FXML
  Label maxPlayerLabel;
  @FXML
  Label mapSizeLabel;
  @FXML
  Label mapDescriptionLabel;
  @FXML
  Pane commentContainer;
  @FXML
  GridPane root;

  @Resource
  MapService mapService;

  @Resource
  I18n i18n;

  public void createPreview(MapBean mapBean) {
    if (mapBean.getTechnicalName() == null) {
      return;
    }
    //TODO implement official map parser to remove this
    if (mapService.isOfficialMap(mapBean.getTechnicalName())) {
      return;
    }

    largeImagePreview.setImage(mapService.loadLargePreview(mapBean.getTechnicalName()));

    mapNameLabel.setText(mapBean.getDisplayName());
    maxPlayerLabel.setText(i18n.get("mapPreview.maxPlayers", mapBean.getPlayers()));
    mapSizeLabel.setText(i18n.get("mapPreview.size", mapBean.getSize().getWidth(), mapBean.getSize().getHeight()));
    mapDescriptionLabel.setText(mapBean.getDescription());

  }

  public Region getRoot() {
    return root;
  }
}
