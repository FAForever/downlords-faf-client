package com.faforever.client.test.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.commons.api.dto.GroupPermission;

import static com.faforever.client.player.SocialStatus.SELF;

public class KickLobbyMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    getBean(ModeratorService.class).closePlayersLobby(player);
  }

  @Override
  protected boolean getVisible() {
    boolean notSelf = !getObject().getSocialStatus().equals(SELF);
    return notSelf & getBean(ModeratorService.class).getPermissions().contains(GroupPermission.ADMIN_KICK_SERVER);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.kickLobby");
  }
}
