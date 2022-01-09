package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.SELF;

public class InvitePlayerMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    getBean(TeamMatchmakingService.class).invitePlayer(player.getUsername());
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    if (player == null) {
      return false;
    }
    return player.getSocialStatus() != SELF && player.getStatus() == PlayerStatus.IDLE;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.inviteToGame");
  }
}
