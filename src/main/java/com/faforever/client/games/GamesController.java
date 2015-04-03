package com.faforever.client.games;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.GameInfo;
import com.faforever.client.legacy.GameState;
import com.faforever.client.legacy.OnGameInfoListener;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
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
  TableView<GameInfo> gamesList;

  @FXML
  TableColumn<GameInfo, String> gameTitleColumn;

  @FXML
  TableColumn<GameInfo, String> playersColumn;

  @FXML
  TableColumn<GameInfo, String> mapNameColumn;

  @FXML
  TableColumn<GameInfo, String> rankingColumn;

  @Autowired
  GameService gameService;

  @Autowired
  I18n i18n;

  @FXML
  void initialize() {
    ladderButtons.selectedToggleProperty().addListener((observable, oldValue, newValue) -> onLadderButtonSelected(newValue));

    gamesList.setEditable(false);
    gameTitleColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        return param.getValue().title;
      }
    });
    playersColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        return String.format(i18n.get("game.players.format"), param.getValue().numPlayers, param.getValue().maxPlayers);
      }
    });
    mapNameColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        return param.getValue().mapname;
      }
    });
    rankingColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        return StringUtils.defaultString(extractRating(param.getValue().title));
      }
    });
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
    if (!GameState.OPEN.equals(gameInfo.state)) {
      return;
    }
    gamesList.getItems().add(gameInfo);
  }
}
