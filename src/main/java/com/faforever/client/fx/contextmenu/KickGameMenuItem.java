package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.util.Assert;
import com.faforever.commons.api.dto.GroupPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class KickGameMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final ModeratorService moderatorService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    moderatorService.closePlayersGame(object);
  }

  @Override
  protected boolean isItemVisible() {
    if (object == null) {
      return false;
    }
    boolean notSelf = !object.getSocialStatus().equals(SELF);
    return notSelf & moderatorService.getPermissions().contains(GroupPermission.ADMIN_KICK_SERVER);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.kickGame");
  }
}
