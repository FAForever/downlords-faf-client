package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ViewReplaysMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final NavigationHandler navigationHandler;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    navigationHandler.navigateTo(new ShowUserReplaysEvent(object.getId()));
  }

  @Override
  protected String getStyleIcon() {
    return "search-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return object != null;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.viewReplays");
  }
}
