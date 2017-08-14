package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.util.RatingUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

  public void setPlayer(Player player) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));

    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), RatingUtil.getRoundedGlobalRating(player));
    playerInfo.setText(playerInfoLocalized);
  }

  public Node getRoot() {
    return playerInfo;
  }
}
