package com.faforever.client.games;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.GameInfo;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.maps.MapPreviewService;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GamesController implements OnGameInfoListener {

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  @FXML
  ToggleGroup ladderButtons;

  @FXML
  TableView<GameInfoBean> gamesTable;

  @FXML
  TableColumn<GameInfoBean, Image> mapPreviewColumn;

  @FXML
  TableColumn<GameInfoBean, String> gameTitleColumn;

  @FXML
  TableColumn<GameInfoBean, String> playersColumn;

  @FXML
  TableColumn<GameInfoBean, String> mapNameColumn;

  @FXML
  TableColumn<GameInfoBean, String> rankingColumn;

  @FXML
  TableColumn<GameInfoBean, GameStatus> gameStatusColumn;

  @Autowired
  GameService gameService;

  @Autowired
  MapPreviewService mapPreviewService;

  @Autowired
  I18n i18n;

  private ObservableMap<Integer, GameInfoBean> gameInfoBeans;

  public GamesController() {
    gameInfoBeans = FXCollections.observableHashMap();
  }

  @FXML
  void initialize() {
    ladderButtons.selectedToggleProperty().addListener((observable, oldValue, newValue) -> onLadderButtonSelected(newValue));

    gamesTable.setEditable(false);
    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasAdded()) {
        gamesTable.getItems().add(change.getValueAdded());
      } else {
        gamesTable.getItems().remove(change.getValueRemoved());
      }
    });

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell());
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      @Override
      protected Image computeValue() {
        return mapPreviewService.loadPreview(param.getValue().getMapName());
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    playersColumn.setCellValueFactory(param -> new NumberOfPlayersBinding(i18n, param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty()));
    mapNameColumn.setCellValueFactory(param -> param.getValue().mapNameProperty());
    rankingColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        // TODO this is not bound to the title property, however, a game's title can't be changed anyway (atm).
        return StringUtils.defaultString(extractRating(param.getValue().getTitle()));
      }
    });
    gameStatusColumn.setCellValueFactory(param -> param.getValue().statusProperty());
  }

  private static String extractRating(String title) {
    Matcher matcher = RATING_PATTERN.matcher(title);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  @PostConstruct
  public void init() {
    gameService.addOnGameInfoListener(this);
  }

  private void onLadderButtonSelected(Toggle button) {
    gameService.publishPotentialPlayer();
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    if (!GameStatus.OPEN.equals(gameInfo.state)) {
      gameInfoBeans.remove(gameInfo.uid);
      return;
    }

    if (!gameInfoBeans.containsKey(gameInfo.uid)) {
      gameInfoBeans.put(gameInfo.uid, new GameInfoBean(gameInfo));
    }
  }
}
