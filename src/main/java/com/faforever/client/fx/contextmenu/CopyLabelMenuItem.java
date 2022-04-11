package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import com.faforever.client.util.ClipboardUtil;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Primary
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CopyLabelMenuItem extends AbstractMenuItem<Label> {

  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "No label has been set");
    ClipboardUtil.copyToClipboard(object.getText());
  }

  @Override
  protected String getStyleIcon() {
    return "copy-icon";
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && !StringUtils.isBlank(object.getText());
  }

  @Override
  protected String getItemText() {
    return i18n.get("copy");
  }
}
