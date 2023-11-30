package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.social.SocialService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AddFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  private final SocialService socialService;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    if (object.getSocialStatus() == FRIEND) {
      socialService.removeFriend(object);
    }
    socialService.addFoe(object);
  }

  @Override
  protected boolean isDisplayed() {
    if (object == null) {
      return false;
    }
    SocialStatus socialStatus = object.getSocialStatus();
    return socialStatus != FOE && socialStatus != SELF;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.addFoe");
  }
}
