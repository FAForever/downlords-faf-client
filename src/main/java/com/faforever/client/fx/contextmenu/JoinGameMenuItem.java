package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.commons.lobby.GameType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class JoinGameMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    joinGameHelper.join(player.getGame());
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    if (player == null) {
      return false;
    }

    SocialStatus socialStatus = player.getSocialStatus();
    PlayerStatus playerStatus = player.getStatus();
    GameBean game = player.getGame();
    return socialStatus != SELF && (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING)
        && game != null && game.getGameType() != GameType.MATCHMAKER;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.joinGame");
  }
}
