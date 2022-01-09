package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ViewReplaysMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final EventBus eventBus;

  @Override
  protected void onClicked() {
    eventBus.post(new ShowUserReplaysEvent(getObject().getId()));
  }

  @Override
  protected boolean isItemVisible() {
    return getUnsafeObject() != null;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.viewReplays");
  }
}
