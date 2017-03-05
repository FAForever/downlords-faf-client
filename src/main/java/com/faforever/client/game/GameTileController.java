package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Objects;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GameTileController implements Controller<Node> {

  private final MapService mapService;
  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  private final ModService modService;
  private final UiService uiService;
  public Label lockIconLabel;
  public Label gameTypeLabel;
  public Node gameCardRoot;
  public Label gameMapLabel;
  public Label gameTitleLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public Label modsLabel;
  public ImageView mapImageView;
  private Consumer<Game> onSelectedListener;
  private Game game;

  @Inject
  public GameTileController(MapService mapService, I18n i18n, JoinGameHelper joinGameHelper, ModService modService, UiService uiService) {
    this.mapService = mapService;
    this.i18n = i18n;
    this.joinGameHelper = joinGameHelper;
    this.modService = modService;
    this.uiService = uiService;
  }

  public void setOnSelectedListener(Consumer<Game> onSelectedListener) {
    this.onSelectedListener = onSelectedListener;
  }

  public void initialize() {
    modsLabel.managedProperty().bind(modsLabel.visibleProperty());
    modsLabel.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
    gameTypeLabel.managedProperty().bind(gameTypeLabel.visibleProperty());
    lockIconLabel.managedProperty().bind(lockIconLabel.visibleProperty());
    joinGameHelper.setParentNode(getRoot());
  }

  public Node getRoot() {
    return gameCardRoot;
  }

  public void setGame(Game game) {
    this.game = game;

    modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredModBean -> Platform.runLater(() -> gameTypeLabel.setText(StringUtils.defaultString(featuredModBean.getDisplayName()))));

    gameTitleLabel.textProperty().bind(game.titleProperty());
    hostLabel.setText(game.getHost());

    gameMapLabel.textProperty().bind(game.mapFolderNameProperty());
    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.players.format", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(),
        game.maxPlayersProperty()
    ));
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE),
        game.mapFolderNameProperty()
    ));

    modsLabel.textProperty().bind(createStringBinding(
        () -> Joiner.on(i18n.get("textSeparator")).join(game.getSimMods().values()),
        game.getSimMods()
    ));

    // TODO display "unknown map" image first since loading may take a while
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL),
        game.mapFolderNameProperty()
    ));

    lockIconLabel.visibleProperty().bind(game.passwordProtectedProperty());

    Tooltip tooltip = new Tooltip();
    Tooltip.install(gameCardRoot, tooltip);
    tooltip.activatedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        GameTooltipController gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
        gameTooltipController.setGameInfoBean(game);
        tooltip.setGraphic(gameTooltipController.getRoot());
      }
    });
    tooltip.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        tooltip.setGraphic(null);
      }
    });
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
