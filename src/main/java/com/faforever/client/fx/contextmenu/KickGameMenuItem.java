package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.commons.api.dto.GroupPermission;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.SELF;

public class KickGameMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    getBean(ModeratorService.class).closePlayersGame(player);
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    if (player == null) {
      return false;
    }

    boolean notSelf = !player.getSocialStatus().equals(SELF);
    return notSelf & getBean(ModeratorService.class).getPermissions().contains(GroupPermission.ADMIN_KICK_SERVER);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.kickGame");
  }
}
