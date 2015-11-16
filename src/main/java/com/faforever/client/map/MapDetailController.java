package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.Comment;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

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

  @Autowired
  MapService mapService;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  I18n i18n;

  public void createPreview(MapInfoBean mapInfoBean) {
    if (mapInfoBean.getTechnicalName() == null) {
      return;
    }
    //TODO implement official map parser to remove this
    if (mapService.isOfficialMap(mapInfoBean.getTechnicalName())) {
      return;
    }

    largeImagePreview.setImage(mapService.loadLargePreview(mapInfoBean.getTechnicalName()));

    mapNameLabel.setText(i18n.get("mapPreview.name", mapInfoBean.getDisplayName()));
    maxPlayerLabel.setText(i18n.get("mapPreview.maxPlayers", mapInfoBean.getPlayers()));
    mapSizeLabel.setText(i18n.get("mapPreview.size", mapInfoBean.getSize()));
    mapDescriptionLabel.setText(mapInfoBean.getDescription());

    commentContainer.getChildren().clear();
    for (Comment comment : mapService.getComments(mapInfoBean.getId())) {
      CommentCardController commentCardController = applicationContext.getBean(CommentCardController.class);
      commentCardController.addComment(comment);
      commentContainer.getChildren().add(commentCardController.getRoot());
    }
  }

  public Region getRoot() {
    return root;
  }
}
