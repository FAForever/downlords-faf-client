package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class InvitePlayerMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final TeamMatchmakingService teamMatchmakingService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    teamMatchmakingService.invitePlayer(object.getUsername());
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && object.getSocialStatus() != SELF && object.getStatus() == PlayerStatus.IDLE;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.inviteToGame");
  }
}
