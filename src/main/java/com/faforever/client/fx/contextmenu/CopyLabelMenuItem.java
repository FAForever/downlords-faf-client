package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CopyLabelMenuItem extends CopyUsernameMenuItem {

  public CopyLabelMenuItem(I18n i18n) {
    super(i18n);
  }

  @Override
  protected String getItemText() {
    return i18n.get("label.copy");
  }
}
