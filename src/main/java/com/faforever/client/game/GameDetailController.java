package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ProgrammingError;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
  public WatchButtonController watchButtonController;
  private ReadOnlyObjectWrapper<Game> game;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener teamsInvalidationListener;
  @SuppressWarnings("FieldCanBeLocal")
  private final InvalidationListener gameStatusInvalidationListener;
  private final WeakInvalidationListener weakTeamListener;
  private final WeakInvalidationListener weakGameStatusListener;
  private Node watchButton;

  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener featuredModInvalidationListener;

  public GameDetailController(I18n i18n, MapService mapService, ModService modService, PlayerService playerService,
                              UiService uiService, JoinGameHelper joinGameHelper) {
    this.i18n = i18n;
    this.mapService = mapService;
    this.modService = modService;
    this.playerService = playerService;
    this.uiService = uiService;
    this.joinGameHelper = joinGameHelper;

    game = new ReadOnlyObjectWrapper<>();

    gameStatusInvalidationListener = observable -> onGameStatusChanged();
    teamsInvalidationListener = observable -> createTeams();
    weakTeamListener = new WeakInvalidationListener(teamsInvalidationListener);
    weakGameStatusListener = new WeakInvalidationListener(gameStatusInvalidationListener);
  }

  public void initialize() {
    gameDetailRoot.parentProperty().addListener(observable -> {
      if (!(gameDetailRoot.getParent() instanceof Pane)) {
        return;
      }
      gameDetailRoot.maxWidthProperty().bind(((Pane) gameDetailRoot.getParent()).widthProperty());
    });
    watchButton = watchButtonController.getRoot();

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

  private void onGameStatusChanged() {
    Game game = this.game.get();
    switch (game.getStatus()) {
      case PLAYING:
        joinButton.setVisible(false);
        watchButton.setVisible(true);
        watchButtonController.setGame(game);
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
    Optional.ofNullable(this.game.get()).ifPresent(oldGame -> {
      Optional.ofNullable(weakTeamListener).ifPresent(listener -> oldGame.getTeams().removeListener(listener));
      Optional.ofNullable(weakGameStatusListener).ifPresent(listener -> oldGame.statusProperty().removeListener(listener));
    });

    this.game.set(game);
    if (game == null) {
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

    featuredModInvalidationListener = observable -> modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredMod -> Platform.runLater(() -> {
          gameTypeLabel.setText(i18n.get("loading"));
          String fullName = featuredMod != null ? featuredMod.getDisplayName() : null;
          gameTypeLabel.setText(StringUtils.defaultString(fullName));
        }));
    game.featuredModProperty().addListener(new WeakInvalidationListener(featuredModInvalidationListener));
    featuredModInvalidationListener.invalidated(game.featuredModProperty());

    JavaFxUtil.addListener(game.getTeams(), weakTeamListener);
    teamsInvalidationListener.invalidated(game.getTeams());

    JavaFxUtil.addListener(game.statusProperty(), weakGameStatusListener);
    gameStatusInvalidationListener.invalidated(game.statusProperty());
  }

  public Game getGame() {
    return game.get();
  }

  public ReadOnlyObjectProperty<Game> gameProperty() {
    return game.getReadOnlyProperty();
  }

  private void createTeams() {
    teamListPane.getChildren().clear();
    ObservableMap<String, List<String>> teams = this.game.get().getTeams();
    synchronized (teams) {
      TeamCardController.createAndAdd(teams, playerService, uiService, teamListPane);
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
