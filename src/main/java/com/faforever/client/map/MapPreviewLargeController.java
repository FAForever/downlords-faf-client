package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.map.Comment;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;

public class MapPreviewLargeController {

  public ImageView largeImagePreview;
  public Label mapNameLabel;
  public Label maxPlayerLabel;
  public Label mapSizeLabel;
  public Label mapDescriptionLabel;
  public Pane commentContainer;
  public GridPane root;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  MapService mapService;

  @Autowired
  ApplicationContext applicationContext;

  public void createPreview(MapInfoBean mapInfoBean) {
    //TODO implement official map parser to remove this
    if(!mapService.isOfficialMap(mapInfoBean.getName())) {
        logger.error("{}",mapInfoBean.getName());
        Image mapPreview = mapService.loadLargePreview(mapInfoBean.getName());
        largeImagePreview.setFitWidth(2 * mapPreview.getWidth());
        largeImagePreview.setImage(mapPreview);

        mapNameLabel.setText(mapInfoBean.getName());
        maxPlayerLabel.setText(Integer.toString(mapInfoBean.getPlayers()));
        mapSizeLabel.setText(mapInfoBean.getSize().toString());
        mapDescriptionLabel.setText(mapInfoBean.getDescription());

        for (Comment comment : mapService.getComments(mapInfoBean.getName())) {
          CommentCardController commentCardController = applicationContext.getBean(CommentCardController.class);
          commentCardController.createComment(comment);
          commentContainer.getChildren().add(commentCardController.getRoot());
        }
      }
    }

  public Region getRoot() {
    return root;
  }
}
