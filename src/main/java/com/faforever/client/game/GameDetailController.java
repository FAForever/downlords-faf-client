package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class GameDetailController implements Controller<Pane> {

  private final I18n i18n;
  private final MapService mapService;
  private final ModService modService;
  private final TimeService timeService;
  private final UiService uiService;
  private final ImageViewHelper imageViewHelper;
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
  public Label playtimeLabel;
  public Node joinButton;
  public WatchButtonController watchButtonController;
  public Node watchButton;

  private GameBean game;
  private boolean playtimeVisible;
  private Timeline playTimeTimeline;

  private InvalidationListener teamsInvalidationListener;
  private InvalidationListener gameStatusInvalidationListener;
  private InvalidationListener gamePropertiesInvalidationListener;
  private InvalidationListener featuredModInvalidationListener;
  private InvalidationListener startTimeInvalidationListener;

  public void initialize() {
    imageViewHelper.setDefaultPlaceholderImage(mapImageView, true);
    contextMenuBuilder.addCopyLabelContextMenu(gameTitleLabel, mapLabel, gameTypeLabel);
    JavaFxUtil.bindManagedToVisible(joinButton, watchButton, gameTitleLabel, hostLabel, mapLabel, numberOfPlayersLabel,
        mapPreviewContainer, gameTypeLabel, playtimeLabel);
    JavaFxUtil.bind(mapPreviewContainer.visibleProperty(), mapImageView.imageProperty().isNotNull());
    gameDetailRoot.parentProperty().addListener(observable -> {
      if (!(gameDetailRoot.getParent() instanceof Pane)) {
        return;
      }
      gameDetailRoot.maxWidthProperty().bind(((Pane) gameDetailRoot.getParent()).widthProperty());
    });
  }

  private void onGameStatusChanged() {
    JavaFxUtil.runLater(() -> {
      if (game != null) {
        switch (game.getStatus()) {
          case PLAYING -> {
            joinButton.setVisible(false);
            calculatePlaytime(game);
          }
          case OPEN -> joinButton.setVisible(true);
          case UNKNOWN, CLOSED -> {
            hideGameDetail();
            stopPlaytime();
          }
        }
      } else {
        stopPlaytime();
      }
    });
  }

  private void calculatePlaytime(GameBean game) {
    if (playtimeVisible && (playTimeTimeline == null || playTimeTimeline.getStatus() == Status.STOPPED)) {
      playtimeLabel.setVisible(true);
      playTimeTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> {
        if (game.getStatus() == GameStatus.PLAYING && game.getStartTime() != null) {
          updatePlaytimeValue(game.getStartTime());
        }
      }), new KeyFrame(Duration.seconds(1)));
      playTimeTimeline.setCycleCount(Timeline.INDEFINITE);
      playTimeTimeline.play();
    }
  }

  private void stopPlaytime() {
    if (playTimeTimeline != null) {
      playTimeTimeline.stop();
    }
    JavaFxUtil.runLater(() -> playtimeLabel.setVisible(false));
  }

  private void updatePlaytimeValue(OffsetDateTime gameStartTime) {
    JavaFxUtil.runLater(() -> playtimeLabel.setText(timeService.shortDuration(java.time.Duration.between(gameStartTime, OffsetDateTime.now()))));
  }

  public void dispose() {
    stopPlaytime();
  }

  private void onGamePropertyChanged() {
    if (game != null) {
      JavaFxUtil.runLater(() -> {
        gameTitleLabel.setText(StringUtils.normalizeSpace(game.getTitle()));
        hostLabel.setText(game.getHost());
        mapLabel.setText(game.getMapFolderName());
        mapImageView.setImage(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE));
      });
    }
  }

  private void onNumPlayersChanged() {
    JavaFxUtil.runLater(() -> {
      if (game != null) {
        numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", game.getNumActivePlayers(), game.getMaxPlayers()));
      }
    });
  }

  public void setGame(GameBean game) {
    resetListeners();

    this.game = game;
    if (game == null || game.getStatus() == GameStatus.CLOSED) {
      hideGameDetail();
      return;
    }

    showGameDetail();

    WeakInvalidationListener weakTeamListener = new WeakInvalidationListener(teamsInvalidationListener);
    WeakInvalidationListener weakGameStatusListener = new WeakInvalidationListener(gameStatusInvalidationListener);
    WeakInvalidationListener weakGamePropertiesListener = new WeakInvalidationListener(gamePropertiesInvalidationListener);
    WeakInvalidationListener weakStartTimeListener = new WeakInvalidationListener(startTimeInvalidationListener);

    JavaFxUtil.addAndTriggerListener(game.featuredModProperty(), new WeakInvalidationListener(featuredModInvalidationListener));
    JavaFxUtil.addListener(game.maxPlayersProperty(), weakTeamListener);
    JavaFxUtil.addAndTriggerListener(game.teamsProperty(), weakTeamListener);
    JavaFxUtil.addAndTriggerListener(game.statusProperty(), weakGameStatusListener);
    JavaFxUtil.addAndTriggerListener(game.titleProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.mapFolderNameProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.hostProperty(), weakGamePropertiesListener);
    JavaFxUtil.addAndTriggerListener(game.startTimeProperty(), weakStartTimeListener);
  }

  public void resetListeners() {
    featuredModInvalidationListener = observable -> onFeaturedModChanged();
    gameStatusInvalidationListener = observable -> onGameStatusChanged();
    teamsInvalidationListener = observable -> {
      createTeams();
      onNumPlayersChanged();
    };
    gamePropertiesInvalidationListener = observable -> onGamePropertyChanged();
    startTimeInvalidationListener = observable -> onStartTimeChanged();
  }

  private void onStartTimeChanged() {
    JavaFxUtil.runLater(() -> {
      if (game != null && game.getStatus() == GameStatus.PLAYING) {
        OffsetDateTime startTime = game.getStartTime();
        if (startTime != null) {
          watchButtonController.setGame(game);
          joinButton.setVisible(false);
        }
        watchButton.setVisible(startTime != null);
      } else {
        joinButton.setVisible(game != null && game.getStatus() == GameStatus.OPEN);
        watchButton.setVisible(false);
      }
    });
  }

  private void onFeaturedModChanged() {
    modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredMod -> {
          String fullName = featuredMod != null ? featuredMod.getDisplayName() : null;
          JavaFxUtil.runLater(() -> gameTypeLabel.setText(StringUtils.defaultString(fullName)));
        });
  }

  private void createTeams() {
    List<Node> teamCardPanes = new ArrayList<>();
    if (game != null) {
      for (Map.Entry<Integer, List<PlayerBean>> entry : game.getTeams().entrySet()) {
        Integer team = entry.getKey();

        if (team != null) {
          TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
          teamCardController.setPlayersInTeam(team, entry.getValue(),
              player -> RatingUtil.getLeaderboardRating(player, game.getLeaderboard()), null, RatingPrecision.ROUNDED);
          teamCardPanes.add(teamCardController.getRoot());
        }
      }
    }
    JavaFxUtil.runLater(() -> teamListPane.getChildren().setAll(teamCardPanes));
  }

  private void showGameDetail() {
    JavaFxUtil.runLater(() -> getRoot().setVisible(true));
  }

  private void hideGameDetail() {
    JavaFxUtil.runLater(() -> getRoot().setVisible(false));
  }

  public void setPlaytimeVisible(boolean visible) {
    playtimeVisible = visible;
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

  @VisibleForTesting
  protected Timeline getPlayTimeTimeline() {
    return playTimeTimeline;
  }
}
