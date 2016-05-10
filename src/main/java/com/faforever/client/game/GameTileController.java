package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.google.common.base.Joiner;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

public class GameTileController {

  @FXML
  Label lockIconLabel;
  @FXML
  Label gameTypeLabel;
  @FXML
  Node gameTileRoot;
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

  @Resource
  MapService mapService;
  @Resource
  I18n i18n;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  GamesController gamesController;
  @Resource
  GameService gameService;

  private GameInfoBean gameInfoBean;

  @FXML
  void initialize() {
    modsLabel.managedProperty().bind(modsLabel.visibleProperty());
    modsLabel.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
    gameTypeLabel.managedProperty().bind(gameTypeLabel.visibleProperty());
    lockIconLabel.managedProperty().bind(lockIconLabel.visibleProperty());
  }

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    this.gameInfoBean = gameInfoBean;

    GameTypeBean gameType = gameService.getGameTypeByString(gameInfoBean.getFeaturedMod());
    String fullName = gameType != null ? gameType.getFullName() : null;
    gameTypeLabel.setText(StringUtils.defaultString(fullName));

    gameTitleLabel.setText(gameInfoBean.getTitle());
    hostLabel.setText(gameInfoBean.getHost());

    gameMapLabel.textProperty().bind(gameInfoBean.mapTechnicalNameProperty());
    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()),
        gameInfoBean.numPlayersProperty(),
        gameInfoBean.maxPlayersProperty()
    ));
    mapImageView.imageProperty().bind(createObjectBinding(() -> mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName()), gameInfoBean.mapTechnicalNameProperty()));

    modsLabel.textProperty().bind(createStringBinding(
        () -> Joiner.on(i18n.get("textSeparator")).join(gameInfoBean.getSimMods().values()),
        gameInfoBean.getSimMods()
    ));

    // TODO display "unknown map" image first since loading may take a while
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName()),
        gameInfoBean.mapTechnicalNameProperty()
    ));

    lockIconLabel.visibleProperty().bind(gameInfoBean.passwordProtectedProperty());

    // TODO move tooltip Y position down 10 pixels
    // TODO create on hover, not always
    GameTooltipController gameTooltipController = applicationContext.getBean(GameTooltipController.class);
    gameTooltipController.setGameInfoBean(gameInfoBean);
    Tooltip tooltip = new Tooltip();
    tooltip.setGraphic(gameTooltipController.getRoot());
    Tooltip.install(gameTileRoot, tooltip);
  }

  @FXML
  void onClick(MouseEvent mouseEvent) {
    gameTileRoot.requestFocus();
    gamesController.setSelectedGame(gameInfoBean);

    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      mouseEvent.consume();
      gamesController.onJoinGame(gameInfoBean, null, mouseEvent.getScreenX(), mouseEvent.getScreenY());
    }
  }

  public Node getRoot() {
    return gameTileRoot;
  }
}
