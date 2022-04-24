package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.vault.replay.WatchButtonController;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class GameDetailController implements Controller<Pane> {

  private final I18n i18n;
  private final MapService mapService;
  private final ModService modService;
  private final PlayerService playerService;
  private final UiService uiService;
  private final JoinGameHelper joinGameHelper;
  private final ContextMenuBuilder contextMenuBuilder;

  public Pane gameDetailRoot;
  public Label gameTypeLabel;
  public Label mapLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public VBox teamListPane;
  public StackPane mapPreviewContainer;
  public ImageView mapImageView;
  public Label gameTitleLabel;
  public Node joinButton;
  public WatchButtonController watchButtonController;
  public Node watchButton;

  private GameBean game;

  private InvalidationListener teamsInvalidationListener;
  private InvalidationListener gameStatusInvalidationListener;
  private InvalidationListener numPlayersInvalidationListener;
  private InvalidationListener gamePropertiesInvalidationListener;
  private InvalidationListener featuredModInvalidationListener;
  private InvalidationListener startTimeInvalidationListener;

  public void initialize() {
    contextMenuBuilder.addCopyLabelContextMenu(gameTitleLabel, mapLabel, gameTypeLabel);
    JavaFxUtil.bindManagedToVisible(joinButton, watchButton, gameTitleLabel, hostLabel, mapLabel, numberOfPlayersLabel,
        mapPreviewContainer, gameTypeLabel);
    JavaFxUtil.bind(mapPreviewContainer.visibleProperty(), mapImageView.imageProperty().isNotNull());
    gameDetailRoot.parentProperty().addListener(observable -> {
      if (!(gameDetailRoot.getParent() instanceof Pane)) {
        return;
      }
      gameDetailRoot.maxWidthProperty().bind(((Pane) gameDetailRoot.getParent()).widthProperty());
    });
  }

  private void onGameStatusChanged() {
    if (game != null) {
      switch (game.getStatus()) {
        case PLAYING -> JavaFxUtil.runLater(() -> joinButton.setVisible(false));
        case OPEN -> JavaFxUtil.runLater(() -> joinButton.setVisible(true));
        case UNKNOWN, CLOSED -> hideGameDetail();
      }
    }
  }

  private void onGamePropertyChanged() {
    if (game != null) {
      JavaFxUtil.runLater(() -> {
        gameTitleLabel.setText(StringUtils.normalizeSpace(game.getTitle()));
        hostLabel.setText(game.getHost());
        mapLabel.setText(game.getMapFolderName());
        // Do not load coop map preview because they do not exist on API vault
        mapImageView.setImage(game.getGameType() != GameType.COOP ? mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE) : null);
      });
    }
  }

  private void onNumPlayersChanged() {
    if (game != null) {
      JavaFxUtil.runLater(() ->
          numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers())));
    }
  }

  public void setGame(GameBean game) {
    resetListeners();

    this.game = game;
    if (game == null) {
      hideGameDetail();
      return;
    }

    showGameDetail();

    WeakInvalidationListener weakTeamListener = new WeakInvalidationListener(teamsInvalidationListener);
    WeakInvalidationListener weakGameStatusListener = new WeakInvalidationListener(gameStatusInvalidationListener);
    WeakInvalidationListener weakGamePropertiesListener = new WeakInvalidationListener(gamePropertiesInvalidationListener);
    WeakInvalidationListener weakNumPlayersListener = new WeakInvalidationListener(numPlayersInvalidationListener);
    WeakInvalidationListener weakStartTimeListener = new WeakInvalidationListener(startTimeInvalidationListener);

    JavaFxUtil.addAndTriggerListener(game.featuredModProperty(), new WeakInvalidationListener(featuredModInvalidationListener));
    JavaFxUtil.addAndTriggerListener(game.teamsProperty(), weakTeamListener);
    JavaFxUtil.addAndTriggerListener(game.statusProperty(), weakGameStatusListener);
    JavaFxUtil.addAndTriggerListener(game.titleProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.mapFolderNameProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.hostProperty(), weakGamePropertiesListener);
    JavaFxUtil.addAndTriggerListener(game.numPlayersProperty(), weakNumPlayersListener);
    JavaFxUtil.addListener(game.maxPlayersProperty(), weakNumPlayersListener);
    JavaFxUtil.addAndTriggerListener(game.startTimeProperty(), weakStartTimeListener);
  }

  public void resetListeners() {
    featuredModInvalidationListener = observable -> onFeaturedModChanged();
    gameStatusInvalidationListener = observable -> onGameStatusChanged();
    teamsInvalidationListener = observable -> createTeams();
    numPlayersInvalidationListener = observable -> onNumPlayersChanged();
    gamePropertiesInvalidationListener = observable -> onGamePropertyChanged();
    startTimeInvalidationListener = observable -> onStartTimeChanged();
  }

  private void onStartTimeChanged() {
    if (game != null && game.getStatus() == GameStatus.PLAYING) {
      OffsetDateTime startTime = game.getStartTime();
      JavaFxUtil.runLater(() -> {
        if (startTime != null) {
          watchButtonController.setGame(game);
          joinButton.setVisible(false);
        }
        watchButton.setVisible(startTime != null);
      });
    } else {
      joinButton.setVisible(game != null && game.getStatus() == GameStatus.OPEN);
      watchButton.setVisible(false);
    }
  }

  private void onFeaturedModChanged() {
    modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredMod -> {
          String fullName = featuredMod != null ? featuredMod.getDisplayName() : null;
          JavaFxUtil.runLater(() -> gameTypeLabel.setText(StringUtils.defaultString(fullName)));
        });
  }

  private void createTeams() {
    TeamCardController.createAndAdd(game, playerService, uiService, teamListPane);
  }

  private void showGameDetail() {
    JavaFxUtil.runLater(() -> getRoot().setVisible(true));
  }

  private void hideGameDetail() {
    JavaFxUtil.runLater(() -> getRoot().setVisible(false));
  }

  @Override
  public Pane getRoot() {
    return gameDetailRoot;
  }

  public void onJoinButtonClicked() {
    joinGameHelper.join(game);
  }

  public void onMapPreviewImageClicked() {
    if (game != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE));
    }
  }
}
