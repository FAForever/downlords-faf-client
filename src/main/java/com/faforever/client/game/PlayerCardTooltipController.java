package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayerCardTooltipController implements Controller<Node> {

  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  public Label playerInfo;
  public ImageView countryImageView;

  @Inject
  public PlayerCardTooltipController(CountryFlagService countryFlagService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.i18n = i18n;
  }

  public void setPlayer(Player player, int rating) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));

    // prevents empty tooltip caused by an empty (non-null) string
    if (!StringUtils.isEmpty(player.getCountry())) {
      // installing tooltip on countryImageView won't work because it's part of label.
      i18n.getCountryNameLocalized(player.getCountry()).ifPresent(country -> Tooltip.install(playerInfo, new Tooltip(country))
      );
    }
    
    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), rating);
    playerInfo.setText(playerInfoLocalized);
  }

  public Node getRoot() {
    return playerInfo;
  }
}
