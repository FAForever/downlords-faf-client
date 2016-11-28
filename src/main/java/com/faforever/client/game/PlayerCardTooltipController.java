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

  public Label playerInfo;
  public ImageView playerFlag;

  @Inject
  CountryFlagService countryFlagService;
  @Inject
  I18n i18n;

  public void setPlayer(Player player) {
    playerFlag.setImage(countryFlagService.loadCountryFlag(player.getCountry()));

    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), RatingUtil.getRoundedGlobalRating(player));
    playerInfo.setText(playerInfoLocalized);
  }

  public Node getRoot() {
    return playerInfo;
  }
}
