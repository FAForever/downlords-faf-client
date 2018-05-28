package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableMap;
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
import org.springframework.util.Assert;

import javax.inject.Inject;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
  private Tooltip tooltip;

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
  }

  public Node getRoot() {
    return gameCardRoot;
  }

  public void setGame(Game game) {
    Assert.isNull(this.game, "Game has already been set");
    this.game = game;

    modService.getFeaturedMod(game.getFeaturedMod())
        .thenAccept(featuredModBean -> Platform.runLater(() -> gameTypeLabel.setText(StringUtils.defaultString(featuredModBean.getDisplayName()))));

    gameTitleLabel.textProperty().bind(game.titleProperty());
    hostLabel.setText(game.getHost());

    StringBinding mapNameBinding = createStringBinding(
        () -> mapService.getMapLocallyFromName(game.getMapFolderName())
            .map(MapBean::getDisplayName)
            .orElse(game.getMapFolderName()),
        game.mapFolderNameProperty());

    JavaFxUtil.bind(gameMapLabel.textProperty(), mapNameBinding);
    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.players.format", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(),
        game.maxPlayersProperty()
    ));
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE),
        game.mapFolderNameProperty()
    ));

    ObservableMap<String, String> simMods = game.getSimMods();
    modsLabel.textProperty().bind(createStringBinding(() -> getSimModsLabelContent(simMods), simMods));

    // TODO display "unknown map" image first since loading may take a while
    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL),
        game.mapFolderNameProperty()
    ));

    lockIconLabel.visibleProperty().bind(game.passwordProtectedProperty());
  }

  private String getSimModsLabelContent(ObservableMap<String, String> simMods) {
    List<String> modNames = simMods.entrySet().stream()
        .limit(2)
        .map(Entry::getValue)
        .collect(Collectors.toList());

    if (simMods.size() > 2) {
      return i18n.get("game.mods.twoAndMore", modNames.get(0), modNames.size());
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

  public void onMouseEntered() {
    GameTooltipController gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    gameTooltipController.setGame(game);

    tooltip = new Tooltip();
    tooltip.setGraphic(gameTooltipController.getRoot());
    Tooltip.install(gameCardRoot, tooltip);
  }

  public void onMouseExited() {
    Tooltip.uninstall(gameCardRoot, tooltip);
    tooltip = null;
  }
}
