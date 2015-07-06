package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.google.common.base.Joiner;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GameCardController {

  private static final long POPUP_DELAY = 1000;

  @FXML
  Label modsPrefix;

  @FXML
  Label gameTypePrefix;

  @FXML
  ImageView mapImageCornerView;

  @FXML
  Label gameType;

  @FXML
  Node gameCardRoot;

  @FXML
  Label gameMapLabel;

  @FXML
  Label gameTitleLabel;

  @FXML
  Label numberOfPlayersLabel;

  @FXML
  Label hosterLabel;

  @FXML
  Label modsLabel;

  @FXML
  ImageView mapImageView;

  @Autowired
  MapService mapService;

  @Autowired
  I18n i18n;

  @Autowired
  Environment environment;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  GamesController gamesController;

  private double lastMouseX;
  private double lastMouseY;
  private OnGameJoinListener onGameJoinListener;
  private GameInfoBean gameInfoBean;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //FIXME change name of cards to tiles
  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    this.gameInfoBean = gameInfoBean;
    gameTitleLabel.setText(gameInfoBean.getTitle());
    hosterLabel.setText(gameInfoBean.getHost());
    if (gameInfoBean.getFeaturedMod().equals(environment.getProperty("defaultMod"))) {
      gameType.setText("");
    } else {
      gameType.setText(gameInfoBean.getFeaturedMod());
      if(!gameType.getText().isEmpty()){
        gameTypePrefix.setVisible(true);
      }
    }

    gameMapLabel.setText(gameInfoBean.getMapName());
    gameInfoBean.mapNameProperty().addListener(((observable3, oldValue3, newValue3) -> {
      gameMapLabel.setText(gameInfoBean.getMapName());
      numberOfPlayersLabel.setText(
          i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers())
      );
    }));

    numberOfPlayersLabel.setText(i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    gameInfoBean.numPlayersProperty().addListener(((observable3, oldValue3, newValue3) -> {
      numberOfPlayersLabel.setText(
          i18n.get("game.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers())
      );
    }));

    displaySimMods(gameInfoBean.getSimMods());
    gameInfoBean.getSimMods().addListener((MapChangeListener<String, String>) change -> {
      displaySimMods(change.getMap());
    });

    Image image = mapService.loadSmallPreview(gameInfoBean.getMapName());
    mapImageView.setImage(image);
    gameInfoBean.mapNameProperty().addListener((observable, oldValue, newValue) -> {
      Image newImage = mapService.loadSmallPreview(newValue);
      mapImageView.setImage(newImage);
    });

    if(gameInfoBean.getAccess() == GameAccess.PASSWORD){
      Image lockImage = new Image("/images/private_game.png");
      mapImageCornerView.setImage(lockImage);
    }

    //TODO move tooltip Y position down 10 pixels
    PopupGamePaneController popupGamePaneController = applicationContext.getBean(PopupGamePaneController.class);
    popupGamePaneController.setGameInfoBean(gameInfoBean);
    Tooltip tooltip = new Tooltip();
    tooltip.setGraphic(popupGamePaneController.getRoot());
    Tooltip.install(gameCardRoot, tooltip);
  }

  //FIXME hacky kind of way in setting up joinSelectedGame
  @FXML
  void onClick(MouseEvent mouseEvent) {
    lastMouseX = mouseEvent.getSceneX();
    lastMouseY = mouseEvent.getScreenY();
    gamesController.displayGameDetail(gameInfoBean);
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      mouseEvent.consume();
      gamesController.joinSelectedGame(null,gameInfoBean, mouseEvent);
      //onGameJoinListener.joinGame(gameInfoBean, lastMouseX, lastMouseY);
    }
  }

  private void displaySimMods(ObservableMap<? extends String, ? extends String> simMods) {
    String stringSimMods = Joiner.on(i18n.get("textSeparator")).join(simMods.values());
    modsLabel.setText(stringSimMods);
    if(!modsLabel.getText().isEmpty()){
      modsPrefix.setVisible(true);
    }
  }

  public Node getRoot() {
    return gameCardRoot;
  }

  public void setOnGameJoinListener(OnGameJoinListener onGameJoinListener) {
    this.onGameJoinListener = onGameJoinListener;
  }
}
