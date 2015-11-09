package com.faforever.client.chat;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

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
  @Autowired
  MapService mapService;
  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  I18n i18n;

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    mapPreview.setImage(mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName()));
    MapInfoBean mapInfoBean = mapService.getMapInfoBeanFromVaultFromName(gameInfoBean.getMapTechnicalName());

    gameTitleLabel.setText(i18n.get("chat.gameStatus.gameTitle", gameInfoBean.getTitle()));
    featuredModLabel.setText(i18n.get("chat.gameStatus.featuredMod", gameInfoBean.getFeaturedMod()));

    if (mapInfoBean != null) {
      mapNameLabel.setText(i18n.get("chat.gameStatus.mapName", mapInfoBean.getDisplayName()));
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
