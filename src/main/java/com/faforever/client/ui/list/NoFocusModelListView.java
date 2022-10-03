package com.faforever.client.ui.list;

import javafx.scene.control.FocusModel;

public class NoFocusModelListView<T> extends FocusModel<T> {

  @Override
  protected int getItemCount() {
    return 0;
  }

  @Override
  protected T getModelItem(int index) {
    return null;
  }
}
