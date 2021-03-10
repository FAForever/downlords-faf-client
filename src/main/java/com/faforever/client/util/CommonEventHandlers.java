package com.faforever.client.util;

import com.faforever.client.mod.ModVersion;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.NonNull;

public class CommonEventHandlers {

  public static class SearchTextFieldKeyEventHandler implements EventHandler<KeyEvent> {
    private ListView listView;
    private FilteredList filteredList;

    public SearchTextFieldKeyEventHandler(@NonNull ListView listView,@NonNull FilteredList filteredList) {
      this.listView = listView;
      this.filteredList = filteredList;
    }

    @Override
    public void handle(KeyEvent event) {
      MultipleSelectionModel<ModVersion> selectionModel = listView.getSelectionModel();
      int currentIndex = selectionModel.getSelectedIndex();
      int newIndex = currentIndex;
      if (KeyCode.DOWN == event.getCode()) {
        if (filteredList.size() > currentIndex + 1) {
          newIndex++;
        }
        event.consume();
      } else if (KeyCode.UP == event.getCode()) {
        if (currentIndex > 0) {
          newIndex--;
        }
        event.consume();
      }
      selectionModel.select(newIndex);
      listView.scrollTo(newIndex);
    }
  }
}
