package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ProgrammingError;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameDetailController implements Controller<Pane> {

  private final I18n i18n;
  private final MapService mapService;
  private final ModService modService;
  private final PlayerService playerService;
  private final UiService uiService;
  private final JoinGameHelper joinGameHelper;

  public Pane gameDetailRoot;
  public Label gameTypeLabel;
  public Label mapLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public VBox teamListPane;
  public ImageView mapImageView;
  public Label gameTitleLabel;
  public Node joinButton;
  public Node watchButton;
  private Game game;
  private InvalidationListener teamsInvalidationListener;
  private InvalidationListener gameStatusInvalidationListener;

  public GameDetailController(I18n i18n, MapService mapService, ModService modService, PlayerService playerService,
                              UiService uiService, JoinGameHelper joinGameHelper) {
    this.i18n = i18n;
    this.mapService = mapService;
    this.modService = modService;
    this.playerService = playerService;
    this.uiService = uiService;
    this.joinGameHelper = joinGameHelper;

    gameStatusInvalidationListener = observable -> onGameStatusChanged();
  }

  public void initialize() {
    joinButton.managedProperty().bind(joinButton.visibleProperty());
    watchButton.managedProperty().bind(watchButton.visibleProperty());
    setGame(null);
  }

  private void onGameStatusChanged() {
    switch (game.getStatus()) {
      case PLAYING:
        joinButton.setVisible(false);
        watchButton.setVisible(true);
        break;
      case OPEN:
        joinButton.setVisible(true);
        watchButton.setVisible(false);
        break;
      case UNKNOWN:
      case CLOSED:
        joinButton.setVisible(false);
        watchButton.setVisible(false);
        break;
      default:
        throw new ProgrammingError("Uncovered status: " + game.getStatus());
    }
  }

  public void setGame(Game game) {
    this.game = game;
    if (game == null) {
      joinButton.setVisible(false);
      watchButton.setVisible(false);
      return;
    }
    gameTitleLabel.textProperty().bind(game.titleProperty());
    hostLabel.textProperty().bind(game.hostProperty());
    mapLabel.textProperty().bind(game.mapFolderNameProperty());
    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(),
        game.maxPlayersProperty()
    ));
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE),
        game.mapFolderNameProperty()
    ));
    gameTypeLabel.textProperty().bind(createStringBinding(() -> {
      FeaturedMod gameType = modService.getFeaturedMod(game.getFeaturedMod()).get();
      String fullName = gameType != null ? gameType.getDisplayName() : null;
      return StringUtils.defaultString(fullName);
    }, game.featuredModProperty()));

    if (this.game != null && teamsInvalidationListener != null) {
      this.game.getTeams().removeListener(teamsInvalidationListener);
    }

    teamsInvalidationListener = observable -> createTeams(game.getTeams());
    teamsInvalidationListener.invalidated(game.getTeams());
    game.getTeams().addListener(new WeakInvalidationListener(teamsInvalidationListener));
    game.statusProperty().addListener(gameStatusInvalidationListener);
    gameStatusInvalidationListener.invalidated(game.statusProperty());
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber) {
    teamListPane.getChildren().clear();
    synchronized (playersByTeamNumber) {
      TeamCardController.createAndAdd(playersByTeamNumber, playerService, uiService, teamListPane);
    }
  }

  @Override
  public Pane getRoot() {
    return gameDetailRoot;
  }

  public void onJoinButtonClicked(ActionEvent event) {
    joinGameHelper.join(game);
  }
}
