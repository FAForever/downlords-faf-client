package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.util.Assert;
import com.faforever.commons.lobby.GameType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class JoinGameMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final GameRunner gameRunner;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    gameRunner.join(object.getGame());
  }

  @Override
  protected boolean isDisplayed() {
    if (object == null) {
      return false;
    }
    SocialStatus socialStatus = object.getSocialStatus();
    PlayerStatus playerStatus = object.getStatus();
    GameBean game = object.getGame();
    return socialStatus != SELF && (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING)
        && game != null && game.getGameType() != GameType.MATCHMAKER;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.joinGame");
  }
}
