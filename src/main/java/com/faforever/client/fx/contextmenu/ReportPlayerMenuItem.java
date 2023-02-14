package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ReportPlayerMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final UiService uiService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(object);
    reportDialogController.setOwnerWindow(getParentPopup().getOwnerWindow());
    reportDialogController.show();
  }

  @Override
  protected String getStyleIcon() {
    return "assignment-late-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && object.getSocialStatus() != SELF;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.report");
  }
}
