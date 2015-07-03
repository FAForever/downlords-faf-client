package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.google.common.base.Joiner;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

public class GameCardController {

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
  CountryFlagService countryFlagService;

  @Autowired
  PlayerService playerService;

  @Autowired
  I18n i18n;

  @Autowired
  Environment environment;

  private double lastMouseX;
  private double lastMouseY;
  private OnGameJoinListener onGameJoinListener;
  private GameInfoBean gameInfoBean;
  private PlayerInfoBean playerInfoBean;
  Popup popup = new Popup();

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    this.gameInfoBean = gameInfoBean;
    gameTitleLabel.setText(gameInfoBean.getTitle());
    hosterLabel.setText(gameInfoBean.getHost());
    if (gameInfoBean.getFeaturedMod().equals(environment.getProperty("defaultMod"))) {
      gameType.setText("");
    } else {
      gameType.setText(gameInfoBean.getFeaturedMod());
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
    gameInfoBean.simModsProperty().addListener((observable, oldValue, newValue) -> {
    });

    Image image = mapService.loadSmallPreview(gameInfoBean.getMapName());
    mapImageView.setImage(image);
    gameInfoBean.mapNameProperty().addListener((observable, oldValue, newValue) -> {
      Image newImage = mapService.loadSmallPreview(newValue);
      mapImageView.setImage(newImage);
    });
  }

  @FXML
  void onClick(MouseEvent mouseEvent) {
    lastMouseX = mouseEvent.getSceneX();
    lastMouseY = mouseEvent.getScreenY();
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      mouseEvent.consume();

      lastMouseX = mouseEvent.getSceneX();
      lastMouseY = mouseEvent.getScreenY();

      onGameJoinListener.joinGame(gameInfoBean, lastMouseX, lastMouseY);
    }
  }

  private void displaySimMods(ObservableMap<String, String> simMods) {
    String stringSimMods = Joiner.on(", ").join(simMods.values());
    modsLabel.setText(stringSimMods);
  }

  public Node getRoot() {
    return gameCardRoot;
  }

  public void setOnGameJoinListener(OnGameJoinListener onGameJoinListener) {
    this.onGameJoinListener = onGameJoinListener;
  }


  public void onHover(Event event) {
    gameInfoBean.teamsProperty().addListener((observable, oldValue, newValue) -> {
      popup.getContent().set(0,new PopupGamePaneController().PopGamePaneController(gameInfoBean));
    });
  }

  public void onLeave(Event event) {
    popup.getContent().removeAll();
  }
}
