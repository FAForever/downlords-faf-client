package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.GameTypeBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    GameTypeBean gameType = gameService.getGameTypeByString(gameInfoBean.getFeaturedMod());
    String fullName = gameType != null ? gameType.getFullName() : null;
    gameTypeLabel.setText(StringUtils.defaultString(fullName));

    gameTitleLabel.setText(gameInfoBean.getTitle());
    hostLabel.setText(gameInfoBean.getHost());

    JavaFxUtil.bindOnApplicationThread(gameMapLabel.textProperty(), gameInfoBean::getMapTechnicalName, gameInfoBean.mapTechnicalNameProperty());
    JavaFxUtil.bindOnApplicationThread(numberOfPlayersLabel.textProperty(), () -> i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()), gameInfoBean.numPlayersProperty());
    JavaFxUtil.bindOnApplicationThread(mapImageView.imageProperty(), () -> mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName()), gameInfoBean.mapTechnicalNameProperty());
    JavaFxUtil.bindOnApplicationThread(modsLabel.textProperty(), () -> Joiner.on(i18n.get("textSeparator")).join(gameInfoBean.getSimMods().values()), gameInfoBean.mapTechnicalNameProperty());
    JavaFxUtil.bindOnApplicationThread(modsLabel.visibleProperty(), () -> !gameInfoBean.getSimMods().isEmpty(), gameInfoBean.mapTechnicalNameProperty());

    numberOfPlayersLabel.setText(i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    gameInfoBean.numPlayersProperty().addListener(((observable3, oldValue3, newValue3) -> {
      Platform.runLater(() -> numberOfPlayersLabel.setText(i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers())));
    }));

    displaySimMods(gameInfoBean.getSimMods());
    gameInfoBean.getSimMods().addListener((MapChangeListener<String, String>) change -> displaySimMods(change.getMap()));

    // TODO display "unknown map" image first since loading may take a while
    Image image = mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName());
    mapImageView.setImage(image);
    gameInfoBean.mapTechnicalNameProperty().addListener((observable, oldValue, newValue) -> {
      Image newImage = mapService.loadSmallPreview(newValue);
      mapImageView.setImage(newImage);
    });

    lockIconLabel.setVisible(gameInfoBean.getPasswordProtected());

    GameTooltipController gameTooltipController = applicationContext.getBean(GameTooltipController.class);
    gameTooltipController.setGameInfoBean(gameInfoBean);
    gameStatusTooltipRoot.getChildren().add(gameTooltipController.getRoot());
    gameStatusTooltipRoot.getChildren();
  }

  private void displaySimMods(ObservableMap<? extends String, ? extends String> simMods) {
      String stringSimMods = Joiner.on(i18n.get("textSeparator")).join(simMods.values());
    Platform.runLater(() -> {
      modsLabel.setText(stringSimMods);
      modsLabel.setVisible(!modsLabel.getText().isEmpty());
    });
  }

  public Node getRoot() {
    return gameStatusTooltipRoot;
  }
}
