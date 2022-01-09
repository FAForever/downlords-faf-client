package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
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
    PlayerBean player = getObject();
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getParentPopup().getOwnerWindow());
    reportDialogController.show();
  }

  @Override
  protected boolean isItemVisible() {
    PlayerBean player = getUnsafeObject();
    return player != null && player.getSocialStatus() != SELF;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.report");
  }
}
