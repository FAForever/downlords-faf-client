package com.faforever.client.test.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;

import static com.faforever.client.player.SocialStatus.FRIEND;

public class RemoveFriendMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    getBean(PlayerService.class).removeFriend(player);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.removeFriend");
  }

  @Override
  protected boolean getVisible() {
    SocialStatus status = getObject().getSocialStatus();
    return status == FRIEND;
  }
}
