package com.faforever.client.test.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

public class AddFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    PlayerService playerService = getBean(PlayerService.class);
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  @Override
  protected boolean getVisible() {
    SocialStatus socialStatus = getObject().getSocialStatus();
    return socialStatus != FOE && socialStatus != SELF;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.addFoe");
  }
}
