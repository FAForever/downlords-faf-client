package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
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
    teamMatchmakingService.invitePlayer(getObject().getUsername());
  }

  @Override
  protected boolean isItemVisible() {
    PlayerBean player = getUnsafeObject();
    if (player == null) {
      return false;
    }
    return player.getSocialStatus() != SELF && player.getStatus() == PlayerStatus.IDLE;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.inviteToGame");
  }
}
