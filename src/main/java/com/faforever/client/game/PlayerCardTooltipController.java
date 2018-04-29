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
    
    if (!StringUtils.isEmpty(player.getCountry())) {
      final Tooltip countryTooltip = new Tooltip(i18n.getCountryNameLocalized(player.getCountry()));
      //countryTooltip.textProperty().bind(player.countryProperty());
      Tooltip.install(playerInfo, countryTooltip); //countryImageView won't work because part of label.
    }
    
    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), rating);
    playerInfo.setText(playerInfoLocalized);
  }

  public Node getRoot() {
    return playerInfo;
  }
}
