package com.faforever.client.game;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.Player;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserContextMenuController;
import com.faforever.commons.api.dto.Faction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayerCardTooltipController implements Controller<Node> {

  @VisibleForTesting
  static final Image RANDOM_IMAGE = new Image("/images/factions/random.png");
  private final CountryFlagService countryFlagService;
  private final UiService uiService;
  private final EventBus eventBus;
  private final I18n i18n;

  public Label playerInfo;
  public ImageView countryImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;

  private WeakReference<UserContextMenuController> playerContextMenuReference;

  public void setPlayer(Player player, Integer rating, Faction faction) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));
    playerInfo.setText(rating != null
        ? i18n.get("userInfo.tooltipFormat.withRating", player.getUsername(), rating)
        : i18n.get("userInfo.tooltipFormat.noRating", player.getUsername()));
    setFactionIcon(faction);
    foeIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FOE, player.socialStatusProperty()));
    friendIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FRIEND, player.socialStatusProperty()));

    if (player.getSocialStatus() != SocialStatus.SELF) {
      getRoot().setOnMouseClicked((event) -> {
        if (event.getButton() == MouseButton.SECONDARY) {
          showContextMenu(event, player);
        } else if (event.getClickCount() == 2) {
          initiatePrivateChat(player);
        }
      });
    }
  }

  private void initiatePrivateChat(Player player) {
    eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
  }

  private void showContextMenu(MouseEvent event, Player player) {
    UserContextMenuController controller = uiService.loadFxml("theme/user/user_context_menu.fxml");
    controller.setPlayer(player);
    controller.getRoot().show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    playerContextMenuReference = new WeakReference<>(controller);
  }

  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(factionImage, factionIcon, foeIconText, friendIconText);
    foeIconText.setTooltip(new Tooltip(i18n.get("userInfo.foe")));
    friendIconText.setTooltip(new Tooltip(i18n.get("userInfo.friend")));
  }

  private void setFactionIcon(Faction faction) {
    if (faction == null) {
      return;
    }

    factionIcon.setVisible(true);
    switch (faction) {
      case AEON -> factionIcon.getStyleClass().add(UiService.AEON_STYLE_CLASS);
      case CYBRAN -> factionIcon.getStyleClass().add(UiService.CYBRAN_STYLE_CLASS);
      case SERAPHIM -> factionIcon.getStyleClass().add(UiService.SERAPHIM_STYLE_CLASS);
      case UEF -> factionIcon.getStyleClass().add(UiService.UEF_STYLE_CLASS);
      default -> {
        factionIcon.setVisible(false);
        factionImage.setVisible(true);
        factionImage.setImage(RANDOM_IMAGE);
      }
    }
  }
}
