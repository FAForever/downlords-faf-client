package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;

public class PartyPlayerItemListCell extends ListCell<Player> {

  private final PartyPlayerItemController controller;

  public PartyPlayerItemListCell(UiService uiService) {
    controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_player_card.fxml");
  }

  @Override
  protected void updateItem(Player player, boolean empty) {
    super.updateItem(player, empty);
    if (player == null || empty) {
      setText(null);
      setGraphic(null);
      return;
    }

    controller.setPlayer(player);
    setGraphic(controller.getRoot());
  }

  @Override
  public void updateSelected(boolean selected) {
    super.updateSelected(selected);
  }
}
