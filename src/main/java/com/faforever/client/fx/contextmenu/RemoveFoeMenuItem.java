package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.social.SocialService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.FOE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemoveFoeMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final SocialService socialService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    socialService.removeFoe(object);
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && object.getSocialStatus() == FOE;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.removeFoe");
  }
}
