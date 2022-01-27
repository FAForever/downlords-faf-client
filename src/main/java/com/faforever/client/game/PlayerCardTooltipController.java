package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
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

import java.util.List;
import java.util.Optional;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayerCardTooltipController implements Controller<Node> {

  @VisibleForTesting
  static final Image RANDOM_IMAGE = new Image("/images/factions/random.png");
  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final I18n i18n;
  public Label playerInfo;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label foeIconText;
  public HBox root;
  public Label friendIconText;
  public Region factionIcon;
  public ImageView factionImage;

  public void setPlayer(PlayerBean player, Integer rating, Faction faction) {
    if (player == null) {
      return;
    }
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresentOrElse(image ->
        countryImageView.setImage(image), () -> countryImageView.setVisible(false));
    Optional.ofNullable(avatarService.loadAvatar(player.getAvatar())).ifPresent(avatarImage -> {
      Tooltip.install(avatarImageView, new Tooltip(player.getAvatar().getDescription()));
      avatarImageView.setImage(avatarImage);
      avatarImageView.setVisible(true);
    });
    playerInfo.setText(rating != null
        ? i18n.get("userInfo.tooltipFormat.withRating", player.getUsername(), rating)
        : i18n.get("userInfo.tooltipFormat.noRating", player.getUsername()));
    setFactionIcon(faction);
    JavaFxUtil.bind(foeIconText.visibleProperty(),
        Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FOE, player.socialStatusProperty()));
    JavaFxUtil.bind(friendIconText.visibleProperty(),
        Bindings.createBooleanBinding(() -> player.getSocialStatus() == SocialStatus.FRIEND, player.socialStatusProperty()));
  }

  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(factionIcon, foeIconText, factionImage, friendIconText, avatarImageView, countryImageView);
  }

  private void setFactionIcon(Faction faction) {
    if (faction == null) {
      return;
    }

    factionIcon.setVisible(true);
    List<String> classes = factionIcon.getStyleClass();
    switch (faction) {
      case AEON -> classes.add(UiService.AEON_STYLE_CLASS);
      case CYBRAN -> classes.add(UiService.CYBRAN_STYLE_CLASS);
      case SERAPHIM -> classes.add(UiService.SERAPHIM_STYLE_CLASS);
      case UEF -> classes.add(UiService.UEF_STYLE_CLASS);
      default -> {
        factionIcon.setVisible(false);
        factionImage.setVisible(true);
        factionImage.setImage(RANDOM_IMAGE);
      }
    }
  }
}
