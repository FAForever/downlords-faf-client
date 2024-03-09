package com.faforever.client.tournament;

import com.faforever.client.domain.api.Tournament;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class TournamentItemListCell extends ListCell<Tournament> {

  private final TournamentListItemController controller;

  public TournamentItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/tournaments/tournament_list_item.fxml");
    setPrefWidth(0);
  }

  @Override
  protected void updateItem(Tournament item, boolean empty) {
    super.updateItem(item, empty);
    if (item == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setTournamentItem(item);
    setGraphic(controller.getRoot());
  }
}
