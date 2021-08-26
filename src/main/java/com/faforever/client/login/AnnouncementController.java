package com.faforever.client.login;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.temporal.TemporalAccessor;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AnnouncementController implements Controller<Pane> {

  public Pane loginAnnouncementRoot;
  public Label titleLabel;
  public Label messageLabel;
  public Label timeLabel;

  private final TimeService timeService;
  private final I18n i18n;

  @Override
  public Pane getRoot() {
    return loginAnnouncementRoot;
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public void setMessage(String message) {
    messageLabel.setText(message);
  }

  public void setTime(TemporalAccessor start, TemporalAccessor end) {
    timeLabel.setText(i18n.get("temporalRange", timeService.asDateTime(start), timeService.asDateTime(end)));
  }
}
