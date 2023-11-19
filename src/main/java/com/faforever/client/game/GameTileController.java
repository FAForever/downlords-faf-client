package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
@Slf4j
public class GameTileController extends NodeController<Node> {

  public static final PseudoClass FRIEND_IN_GAME_PSEUDO_CLASS = PseudoClass.getPseudoClass("friendInGame");

  private final MapService mapService;
  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  private final ModService modService;
  private final PlayerService playerService;
  private final AvatarService avatarService;
  private final UiService uiService;
  private final ImageViewHelper imageViewHelper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Node lockIconLabel;
  public Label gameTypeLabel;
  public Node gameCardRoot;
  public Label gameMapLabel;
  public Label gameTitleLabel;
  public Label numberOfPlayersLabel;
  public Label avgRatingLabel;
  public Label hostLabel;
  public ImageView avatarImageView;
  public Region defaultHostIcon;
  public Label modsLabel;
  public ImageView mapImageView;
  private Consumer<GameBean> onSelectedListener;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();

  public void setOnSelectedListener(Consumer<GameBean> onSelectedListener) {
    this.onSelectedListener = onSelectedListener;
  }

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(modsLabel, gameTypeLabel, lockIconLabel, defaultHostIcon, avatarImageView);
    JavaFxUtil.bind(modsLabel.visibleProperty(), modsLabel.textProperty().isNotEmpty());
    JavaFxUtil.bind(defaultHostIcon.visibleProperty(), avatarImageView.imageProperty().isNull());
    JavaFxUtil.bind(avatarImageView.visibleProperty(), avatarImageView.imageProperty().isNotNull());

    ObservableValue<Boolean> showing = uiService.createShowingProperty(getRoot());

    gameTitleLabel.textProperty()
        .bind(game.flatMap(GameBean::titleProperty).map(StringUtils::normalizeSpace).when(showing));
    hostLabel.textProperty().bind(game.flatMap(GameBean::hostProperty).when(showing));
    avatarImageView.imageProperty()
        .bind(game.flatMap(GameBean::hostProperty)
                  .map(playerService::getPlayerByNameIfOnline)
            .map(optional -> optional.orElse(null))
                  .flatMap(PlayerBean::avatarProperty)
                  .map(avatarService::loadAvatar)
            .when(showing));
    gameMapLabel.textProperty().bind(game.flatMap(GameBean::mapFolderNameProperty).when(showing));
    mapImageView.imageProperty()
        .bind(game.flatMap(GameBean::mapFolderNameProperty)
            .flatMap(mapFolderName -> Bindings.createObjectBinding(() -> mapService.loadPreview(mapFolderName, PreviewSize.SMALL), mapService.isInstalledBinding(mapFolderName)))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));
    modsLabel.textProperty()
        .bind(game.flatMap(GameBean::simModsProperty).map(this::getSimModsLabelContent).when(showing));
    lockIconLabel.visibleProperty().bind(game.flatMap(GameBean::passwordProtectedProperty).when(showing));
    numberOfPlayersLabel.textProperty()
        .bind(game.flatMap(gameValue -> Bindings.createStringBinding(() -> i18n.get("game.detail.players.format", gameValue.getNumActivePlayers(), gameValue.getMaxPlayers()), gameValue.numActivePlayersProperty(), gameValue.maxPlayersProperty())
            .when(showing)));
    avgRatingLabel.textProperty()
        .bind(game.flatMap(playerService::getAverageRatingPropertyForGame)
            .map(average -> Math.round(average / 100.0) * 100.0)
            .map(roundedAverage -> i18n.get("game.avgRating.format", roundedAverage)));
    game.addListener((SimpleChangeListener<GameBean>) this::onGamePropertyChanged);
  }

  @Override
  public Node getRoot() {
    return gameCardRoot;
  }

  private void onGamePropertyChanged(GameBean newValue) {
    getRoot().pseudoClassStateChanged(FRIEND_IN_GAME_PSEUDO_CLASS, playerService.areFriendsInGame(newValue));

    modService.getFeaturedMod(newValue.getFeaturedMod())
        .map(FeaturedModBean::getDisplayName)
        .map(StringUtils::defaultString)
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(gameTypeLabel::setText, throwable -> log.error("Unable to set game type label", throwable));
  }

  public void setGame(GameBean game) {
    this.game.set(game);
  }

  private String getSimModsLabelContent(Map<String, String> simMods) {
    List<String> modNames = simMods.values().stream().limit(2).collect(Collectors.toList());

    if (simMods.size() > 2) {
      return i18n.get("game.mods.twoAndMore", modNames.get(0), simMods.size() - 1);
    }
    return Joiner.on(i18n.get("textSeparator")).join(modNames);
  }

  public void onClick(MouseEvent mouseEvent) {
    Objects.requireNonNull(onSelectedListener, "onSelectedListener has not been set");
    Objects.requireNonNull(game, "gameInfoBean has not been set");

    gameCardRoot.requestFocus();
    GameBean gameValue = game.get();
    onSelectedListener.accept(gameValue);

    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      mouseEvent.consume();
      joinGameHelper.join(gameValue);
    }
  }
}
