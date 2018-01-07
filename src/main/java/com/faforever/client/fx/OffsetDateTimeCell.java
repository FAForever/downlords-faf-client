package com.faforever.client.fx;

import com.faforever.client.util.TimeService;
import javafx.scene.control.TableCell;

import java.time.OffsetDateTime;

public class OffsetDateTimeCell<S> extends TableCell<S, OffsetDateTime> {
  private final TimeService timeService;

  public OffsetDateTimeCell(TimeService timeService) {
    this.timeService = timeService;
  }

  @Override
  protected void updateItem(OffsetDateTime item, boolean empty) {
    super.updateItem(item, empty);
    if (!empty) {
      setText(timeService.asDate(item));
    }
  }
}
