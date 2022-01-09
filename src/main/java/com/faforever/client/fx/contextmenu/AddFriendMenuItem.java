package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

public class AddFriendMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    PlayerService playerService = getBean(PlayerService.class);
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.addFriend");
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    if (player == null) {
      return false;
    }

    SocialStatus status = player.getSocialStatus();
    return status != FRIEND && status != SELF;
  }
}
