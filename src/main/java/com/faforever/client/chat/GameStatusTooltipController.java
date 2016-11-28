package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GameStatusTooltipController  implements Controller<Node> {

  public Label lockIconLabel;
  public Label gameTypeLabel;
  public Label gameMapLabel;
  public Label gameTitleLabel;
  public Label numberOfPlayersLabel;
  public Label hostLabel;
  public Label modsLabel;
  public ImageView mapImageView;
  public Pane gameStatusTooltipRoot;

  @Inject
  MapService mapService;
  @Inject
  GameService gameService;
  @Inject
  I18n i18n;
  @Inject
  ModService modService;
  @Inject
  UiService uiService;

  public void initialize() {
    modsLabel.managedProperty().bindBidirectional(modsLabel.visibleProperty());
    modsLabel.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
  }

  public void setGameInfoBean(Game game) {
    gameTypeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      FeaturedModBean gameType = modService.getFeaturedMod(game.getFeaturedMod()).get();
      String fullName = gameType != null ? gameType.getDisplayName() : null;
      return StringUtils.defaultString(fullName);
    }, game.featuredModProperty()));

    gameTitleLabel.textProperty().bind(game.titleProperty());
    hostLabel.textProperty().bind(game.hostProperty());
    gameMapLabel.textProperty().bind(game.mapFolderNameProperty());
    numberOfPlayersLabel.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("game.players.format", game.getNumPlayers(), game.getMaxPlayers()), game.numPlayersProperty()));
    modsLabel.textProperty().bind(Bindings.createStringBinding(() -> Joiner.on(i18n.get("textSeparator")).join(game.getSimMods().values()), game.mapFolderNameProperty()));
    lockIconLabel.visibleProperty().bind(game.passwordProtectedProperty());

    // TODO display "unknown map" image first since loading may take a while
    mapImageView.imageProperty().bind(Bindings.createObjectBinding(() -> mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL), game.mapFolderNameProperty()));

    GameTooltipController gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    gameTooltipController.setGameInfoBean(game);
    gameStatusTooltipRoot.getChildren().add(gameTooltipController.getRoot());
    gameStatusTooltipRoot.getChildren();
  }

  public Node getRoot() {
    return gameStatusTooltipRoot;
  }
}
