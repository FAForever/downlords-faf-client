package com.faforever.client.chat;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.FeaturedModBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

public class GameStatusTooltipController {

  @FXML
  Label lockIconLabel;
  @FXML
  Label gameTypeLabel;
  @FXML
  Label gameMapLabel;
  @FXML
  Label gameTitleLabel;
  @FXML
  Label numberOfPlayersLabel;
  @FXML
  Label hostLabel;
  @FXML
  Label modsLabel;
  @FXML
  ImageView mapImageView;
  @FXML
  Pane gameStatusTooltipRoot;

  @Resource
  MapService mapService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  GameService gameService;
  @Resource
  I18n i18n;

  @FXML
  void initialize() {
    modsLabel.managedProperty().bindBidirectional(modsLabel.visibleProperty());
    modsLabel.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
  }

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    gameTypeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      FeaturedModBean gameType = gameService.getFeaturedMod(gameInfoBean.getFeaturedMod()).get();
      String fullName = gameType != null ? gameType.getDisplayName() : null;
      return StringUtils.defaultString(fullName);
    }, gameInfoBean.featuredModProperty()));

    gameTitleLabel.textProperty().bind(gameInfoBean.titleProperty());
    hostLabel.textProperty().bind(gameInfoBean.hostProperty());
    gameMapLabel.textProperty().bind(gameInfoBean.mapFolderNameProperty());
    numberOfPlayersLabel.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()), gameInfoBean.numPlayersProperty()));
    modsLabel.textProperty().bind(Bindings.createStringBinding(() -> Joiner.on(i18n.get("textSeparator")).join(gameInfoBean.getSimMods().values()), gameInfoBean.mapFolderNameProperty()));
    lockIconLabel.visibleProperty().bind(gameInfoBean.passwordProtectedProperty());

    // TODO display "unknown map" image first since loading may take a while
    mapImageView.imageProperty().bind(Bindings.createObjectBinding(() -> mapService.loadSmallPreview(gameInfoBean.getMapFolderName()), gameInfoBean.mapFolderNameProperty()));

    GameTooltipController gameTooltipController = applicationContext.getBean(GameTooltipController.class);
    gameTooltipController.setGameInfoBean(gameInfoBean);
    gameStatusTooltipRoot.getChildren().add(gameTooltipController.getRoot());
    gameStatusTooltipRoot.getChildren();
  }

  public Node getRoot() {
    return gameStatusTooltipRoot;
  }
}
