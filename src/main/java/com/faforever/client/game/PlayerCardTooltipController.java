package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.SocialStatus;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayerCardTooltipController implements Controller<Node> {

  @VisibleForTesting
  static final Image RANDOM_IMAGE = new Image("/images/factions/random.png");
  private static final String AEON = "aeon";
  private static final String CYBRAN = "cybran";
  private static final String SERAPHIM = "seraphim";
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  private static final String UEF = "uef";
  public Label playerInfo;
  public ImageView countryImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;

  public void setPlayer(Player player, int rating, Faction faction) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));

    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), rating);
    setFactionIcon(faction);
    playerInfo.setText(playerInfoLocalized);
    foeIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FOE, player.socialStatusProperty()));
    friendIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FRIEND, player.socialStatusProperty()));
  }

  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    factionImage.managedProperty().bind(factionImage.visibleProperty());
    factionIcon.managedProperty().bind(factionIcon.visibleProperty());
    foeIconText.managedProperty().bind(foeIconText.visibleProperty());
    foeIconText.setTooltip(new Tooltip(i18n.get("userInfo.foe")));
    friendIconText.managedProperty().bind(friendIconText.visibleProperty());
    friendIconText.setTooltip(new Tooltip(i18n.get("userInfo.friend")));
  }

  private void setFactionIcon(Faction faction) {
    if (faction == null) {
      return;
    }

    factionIcon.setVisible(true);
    switch (faction) {
      case AEON:
        factionIcon.getStyleClass().add(AEON);
        break;
      case CYBRAN:
        factionIcon.getStyleClass().add(CYBRAN);
        break;
      case SERAPHIM:
        factionIcon.getStyleClass().add(SERAPHIM);
        break;
      case UEF:
        factionIcon.getStyleClass().add(UEF);
        break;
      default:
        factionIcon.setVisible(false);
        factionImage.setVisible(true);
        factionImage.setImage(RANDOM_IMAGE);
        break;
    }
  }
}
