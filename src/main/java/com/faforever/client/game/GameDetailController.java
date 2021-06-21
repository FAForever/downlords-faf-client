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
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
  private final ReadOnlyObjectWrapper<Game> game;
  private InvalidationListener teamsInvalidationListener;
  private InvalidationListener gameStatusInvalidationListener;
  private InvalidationListener numPlayersInvalidationListener;
  private InvalidationListener gamePropertiesInvalidationListener;
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
  }

  public void initialize() {
    watchButton = watchButtonController.getRoot();

    JavaFxUtil.addLabelContextMenus(uiService, gameTitleLabel, mapLabel, gameTypeLabel);
    JavaFxUtil.bindManagedToVisible(joinButton, watchButton, gameTitleLabel, hostLabel, mapLabel, numberOfPlayersLabel,
        mapImageView, gameTypeLabel);
    gameDetailRoot.parentProperty().addListener(observable -> {
      if (!(gameDetailRoot.getParent() instanceof Pane)) {
        return;
      }
      gameDetailRoot.maxWidthProperty().bind(((Pane) gameDetailRoot.getParent()).widthProperty());
    });

    gameTitleLabel.visibleProperty().bind(game.isNotNull());
    hostLabel.visibleProperty().bind(game.isNotNull());
    mapLabel.visibleProperty().bind(game.isNotNull());
    numberOfPlayersLabel.visibleProperty().bind(game.isNotNull());
    mapImageView.visibleProperty().bind(game.isNotNull());
    gameTypeLabel.visibleProperty().bind(game.isNotNull());

    setGame(null);
  }

  private void onGameStatusChanged() {
    Game game = getGame();
    switch (game.getStatus()) {
      case PLAYING -> {
        JavaFxUtil.runLater(() -> {
          joinButton.setVisible(false);
          watchButton.setVisible(true);
        });
        watchButtonController.setGame(game);
      }
      case OPEN -> JavaFxUtil.runLater(() -> {
        joinButton.setVisible(true);
        watchButton.setVisible(false);
      });
      case UNKNOWN, CLOSED -> JavaFxUtil.runLater(() -> {
        joinButton.setVisible(false);
        watchButton.setVisible(false);
      });
      default -> throw new ProgrammingError("Uncovered status: " + game.getStatus());
    }
  }

  private void onGamePropertyChanged() {
    Game game = getGame();
    JavaFxUtil.runLater(() -> {
      gameTitleLabel.setText(game.getTitle());
      hostLabel.setText(game.getHost());
      mapLabel.setText(game.getMapFolderName());
      mapImageView.setImage(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE));
    });
  }

  private void onNumPlayersChanged() {
    Game game = getGame();
    JavaFxUtil.runLater(() ->
        numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers())));
  }

  public void setGame(Game game) {
    resetListeners();

    this.game.set(game);
    if (game == null) {
      return;
    }

    WeakInvalidationListener weakTeamListener = new WeakInvalidationListener(teamsInvalidationListener);
    WeakInvalidationListener weakGameStatusListener = new WeakInvalidationListener(gameStatusInvalidationListener);
    WeakInvalidationListener weakGamePropertiesListener = new WeakInvalidationListener(gamePropertiesInvalidationListener);
    WeakInvalidationListener weakNumPlayersListener = new WeakInvalidationListener(numPlayersInvalidationListener);

    JavaFxUtil.addAndTriggerListener(game.featuredModProperty(), new WeakInvalidationListener(featuredModInvalidationListener));
    JavaFxUtil.addAndTriggerListener(game.getTeams(), weakTeamListener);
    JavaFxUtil.addAndTriggerListener(game.statusProperty(), weakGameStatusListener);
    JavaFxUtil.addAndTriggerListener(game.titleProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.mapFolderNameProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.hostProperty(), weakGamePropertiesListener);
    JavaFxUtil.addAndTriggerListener(game.numPlayersProperty(), weakNumPlayersListener);
    JavaFxUtil.addListener(game.maxPlayersProperty(), weakNumPlayersListener);
  }

  public void resetListeners() {
    featuredModInvalidationListener = observable -> onFeaturedModChanged();
    gameStatusInvalidationListener = observable -> onGameStatusChanged();
    teamsInvalidationListener = observable -> createTeams();
    numPlayersInvalidationListener = observable -> onNumPlayersChanged();
    gamePropertiesInvalidationListener = observable -> onGamePropertyChanged();
  }

  private void onFeaturedModChanged() {
    modService.getFeaturedMod(game.get().getFeaturedMod())
        .thenAccept(featuredMod -> {
          String fullName = featuredMod != null ? featuredMod.getDisplayName() : null;
          JavaFxUtil.runLater(() -> {
            gameTypeLabel.setText(i18n.get("loading"));
            gameTypeLabel.setText(StringUtils.defaultString(fullName));
          });
        });
  }

  public Game getGame() {
    return game.get();
  }

  public ReadOnlyObjectProperty<Game> gameProperty() {
    return game.getReadOnlyProperty();
  }

  private void createTeams() {
    TeamCardController.createAndAdd(getGame(), playerService, uiService, teamListPane);
  }

  @Override
  public Pane getRoot() {
    return gameDetailRoot;
  }

  public void onJoinButtonClicked(ActionEvent event) {
    joinGameHelper.join(getGame());
  }
}
