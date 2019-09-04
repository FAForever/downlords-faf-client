package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.SocialStatus;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayerCardTooltipController implements Controller<Node>, DisposableBean {

  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  public Label playerInfo;
  public ImageView countryImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;

  private final Tooltip foeTooltip;
  private final Tooltip friendTooltip;

  @Inject
  public PlayerCardTooltipController(CountryFlagService countryFlagService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.i18n = i18n;

    foeTooltip = new Tooltip(i18n.get("userInfo.foe"));
    friendTooltip = new Tooltip(i18n.get("userInfo.friend"));
  }

  public void setPlayer(Player player, int rating) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));

    String playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.getUsername(), rating);
    playerInfo.setText(playerInfoLocalized);
    foeIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FOE, player.socialStatusProperty()));
    friendIconText.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FRIEND, player.socialStatusProperty()));
  }

  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    foeIconText.managedProperty().bind(foeIconText.visibleProperty());
    Tooltip.install(foeIconText, foeTooltip);
    friendIconText.managedProperty().bind(friendIconText.visibleProperty());
    Tooltip.install(friendIconText, friendTooltip);
  }

  @Override
  public void destroy() throws Exception {
    Tooltip.uninstall(foeIconText, foeTooltip);
    Tooltip.uninstall(friendIconText, friendTooltip);
  }
}
