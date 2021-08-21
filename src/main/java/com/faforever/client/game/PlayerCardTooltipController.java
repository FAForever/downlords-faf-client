package com.faforever.client.game;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.Faction;
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
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  public Label playerInfo;
  public ImageView countryImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;

  public void setPlayer(PlayerBean player, Integer rating, Faction faction) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));

    String playerInfoLocalized;
    if (rating != null) {
      playerInfoLocalized = i18n.get("userInfo.tooltipFormat.withRating", player.getUsername(), rating);
    } else {
      playerInfoLocalized = i18n.get("userInfo.tooltipFormat.noRating", player.getUsername());
    }
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
