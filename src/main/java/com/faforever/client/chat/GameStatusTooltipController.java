package com.faforever.client.chat;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.map.MapService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

public class GameStatusTooltipController {

  @FXML
  Label mapNameLabel;
  @FXML
  Label gameTitleLabel;
  @FXML
  Label featuredModLabel;
  @FXML
  ImageView mapPreview;
  @FXML
  VBox gameStatusTooltipRoot;

  @Resource
  MapService mapService;
  @Resource
  ApplicationContext applicationContext;

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    mapPreview.setImage(mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName()));
    MapInfoBean mapInfoBean = mapService.getMapInfoBeanFromVaultByName(gameInfoBean.getMapTechnicalName());

    gameTitleLabel.setText(gameInfoBean.getTitle());
    featuredModLabel.setText(gameInfoBean.getFeaturedMod());

    if (mapInfoBean != null) {
      mapNameLabel.setText(mapInfoBean.getDisplayName());
    }

    GameTooltipController gameTooltipController = applicationContext.getBean(GameTooltipController.class);
    gameTooltipController.setGameInfoBean(gameInfoBean);
    gameStatusTooltipRoot.getChildren().add(gameTooltipController.getRoot());
    gameStatusTooltipRoot.getChildren();
  }

  public Node getRoot() {
    return gameStatusTooltipRoot;
  }
}
