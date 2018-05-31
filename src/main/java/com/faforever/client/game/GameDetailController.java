package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ProgrammingError;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Slf4j
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
  private ObjectProperty<Game> game;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener teamsInvalidationListener;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener gameStatusInvalidationListener;
  private WeakInvalidationListener weakTeamListener;
  private WeakInvalidationListener weakGameStatusListener;

  public GameDetailController(I18n i18n, MapService mapService, ModService modService, PlayerService playerService,
                              UiService uiService, JoinGameHelper joinGameHelper) {
    this.i18n = i18n;
    this.mapService = mapService;
    this.modService = modService;
    this.playerService = playerService;
    this.uiService = uiService;
    this.joinGameHelper = joinGameHelper;

    game = new SimpleObjectProperty<>();
  }

  public void initialize() {
    joinButton.managedProperty().bind(joinButton.visibleProperty());
    watchButton.managedProperty().bind(watchButton.visibleProperty());
    gameTitleLabel.managedProperty().bind(gameTitleLabel.visibleProperty());
    hostLabel.managedProperty().bind(hostLabel.visibleProperty());
    mapLabel.managedProperty().bind(mapLabel.visibleProperty());
    numberOfPlayersLabel.managedProperty().bind(numberOfPlayersLabel.visibleProperty());
    mapImageView.managedProperty().bind(mapImageView.visibleProperty());
    gameTypeLabel.managedProperty().bind(gameTypeLabel.visibleProperty());

    gameTitleLabel.visibleProperty().bind(game.isNotNull());
    hostLabel.visibleProperty().bind(game.isNotNull());
    mapLabel.visibleProperty().bind(game.isNotNull());
    numberOfPlayersLabel.visibleProperty().bind(game.isNotNull());
    mapImageView.visibleProperty().bind(game.isNotNull());
    gameTypeLabel.visibleProperty().bind(game.isNotNull());

    setGame(null);
  }

  private void onGameStatusChanged(Game game) {
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
    this.game.set(game);
    if (game == null) {
      return;
    }

    gameStatusInvalidationListener = observable -> onGameStatusChanged(game);
    teamsInvalidationListener = observable -> createTeams(game.getTeams(), game);

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

    Optional.ofNullable(weakGameStatusListener).ifPresent(listener -> game.getTeams().removeListener(listener));
    Optional.ofNullable(weakTeamListener).ifPresent(listener -> game.statusProperty().removeListener(listener));


    weakTeamListener = new WeakInvalidationListener(teamsInvalidationListener);
    JavaFxUtil.addListener(game.getTeams(), weakTeamListener);
    teamsInvalidationListener.invalidated(game.getTeams());

    weakGameStatusListener = new WeakInvalidationListener(gameStatusInvalidationListener);
    JavaFxUtil.addListener(game.statusProperty(), weakGameStatusListener);
    gameStatusInvalidationListener.invalidated(game.statusProperty());

  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber, Game game) {
    if (!game.equals(this.game.get())) {
      log.warn("Wrong game updated");
      return;
    }
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
    joinGameHelper.join(game.get());
  }
}
