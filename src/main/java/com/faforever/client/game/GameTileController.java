package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.google.common.base.Joiner;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTileController implements Controller<Node> {

  public static final PseudoClass FRIEND_IN_GAME_PSEUDO_CLASS = PseudoClass.getPseudoClass("friendInGame");

  private final MapService mapService;
  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  private final ModService modService;
  private final PlayerService playerService;
  public Node lockIconLabel;
  public Label gameTypeLabel;
  public Node gameCardRoot;
  public Label gameMapLabel;
  public Label gameTitleLabel;
  public Label numberOfPlayersLabel;
  public Label avgRatingLabel;
  public Label hostLabel;
  public Label modsLabel;
  public ImageView mapImageView;
  private Consumer<Game> onSelectedListener;
  private Game game;

  private InvalidationListener numPlayersInvalidationListener;
  private InvalidationListener gamePropertiesInvalidationListener;

  public void setOnSelectedListener(Consumer<Game> onSelectedListener) {
    this.onSelectedListener = onSelectedListener;
  }

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(modsLabel, gameTypeLabel, lockIconLabel);
    modsLabel.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());

    numPlayersInvalidationListener = observable -> onNumPlayersChanged();
    gamePropertiesInvalidationListener = observable -> onGamePropertyChanged();
  }

  public Node getRoot() {
    return gameCardRoot;
  }

  private void onGamePropertyChanged() {
    JavaFxUtil.runLater(() -> {
      gameTitleLabel.setText(game.getTitle());
      hostLabel.setText(game.getHost());
      gameMapLabel.setText(game.getMapFolderName());
      mapImageView.setImage(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE));
      modsLabel.setText(getSimModsLabelContent(game.getSimMods()));
      lockIconLabel.setVisible(game.isPasswordProtected());
    });
  }

  private void onNumPlayersChanged() {
    boolean friendsInGame = playerService.areFriendsInGame(game);
    JavaFxUtil.runLater(() -> {
      numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers()));
      avgRatingLabel.setText(i18n.get("game.avgRating.format", Math.round(game.getAverageRating() / 100.0) * 100.0));
      getRoot().pseudoClassStateChanged(FRIEND_IN_GAME_PSEUDO_CLASS, friendsInGame);
    });
  }

  public void setGame(Game game) {
    Assert.isNull(this.game, "Game has already been set");
    this.game = game;

    modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredModBean -> JavaFxUtil.runLater(() -> gameTypeLabel.setText(StringUtils.defaultString(featuredModBean.getDisplayName()))));

    WeakInvalidationListener weakGamePropertiesListener = new WeakInvalidationListener(gamePropertiesInvalidationListener);
    WeakInvalidationListener weakNumPlayersListener = new WeakInvalidationListener(numPlayersInvalidationListener);

    JavaFxUtil.addListener(game.titleProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.mapFolderNameProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.hostProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.getSimMods(), weakGamePropertiesListener);
    JavaFxUtil.addAndTriggerListener(game.passwordProtectedProperty(), weakGamePropertiesListener);
    JavaFxUtil.addListener(game.numPlayersProperty(), weakNumPlayersListener);
    JavaFxUtil.addListener(game.maxPlayersProperty(), weakNumPlayersListener);
    JavaFxUtil.addAndTriggerListener(game.averageRatingProperty(), weakNumPlayersListener);
  }

  private String getSimModsLabelContent(ObservableMap<String, String> simMods) {
    List<String> modNames;
    synchronized (simMods) {
      modNames = simMods.values().stream()
          .limit(2)
          .collect(Collectors.toList());
    }

    if (simMods.size() > 2) {
      return i18n.get("game.mods.twoAndMore", modNames.get(0), simMods.size() - 1);
    }
    return Joiner.on(i18n.get("textSeparator")).join(modNames);
  }

  public void onClick(MouseEvent mouseEvent) {
    Objects.requireNonNull(onSelectedListener, "onSelectedListener has not been set");
    Objects.requireNonNull(game, "gameInfoBean has not been set");

    gameCardRoot.requestFocus();
    onSelectedListener.accept(game);

    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      mouseEvent.consume();
      joinGameHelper.join(game);
    }
  }
}
