package com.faforever.client.fx;

import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Alternative to {@link StringListCell} that allows to display to differently styled strings in each cell.
 */
public class DualStringListCell<T> extends ListCell<T> {
  protected Label left = new Label();
  protected Label right = new Label();
  protected Region spacer = new Region();
  
  private HBox container = new HBox(left, spacer, right);

  private Function<T, String> functionLeft;
  private Function<T, String> functionRight;
  
  /**
   * @param growSpacer - If true, space between left and right string will be maximized.
   */
  public DualStringListCell(Function<T, String> functionLeft, Function<T, String> functionRight, boolean growSpacer) {
    super();
    this.functionLeft = functionLeft;
    this.functionRight = functionRight;
    if (growSpacer) {
      HBox.setHgrow(spacer, Priority.ALWAYS);
    }
    init();
  }
  
  /**
   * Customize {@link #left}, {@link #right}, {@link #spacer}.
   */
  protected void init() {}

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    Platform.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        left.setText(functionLeft.apply(item));
        right.setText(functionRight.apply(item));
        // copy font styles
        left.setFont(getFont());
        right.setFont(getFont());
        setGraphic(container);
      }
    });
  }
}