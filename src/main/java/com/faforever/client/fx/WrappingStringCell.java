package com.faforever.client.fx;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class WrappingStringCell<S, T> extends TableCell<S, T> {

  private final Function<T, String> function;
  private final String[] cssClasses;

  public WrappingStringCell(Function<T, String> function) {
    this(function, new String[0]);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setGraphic(null);
    } else {
      Text text = new Text(function.apply(item));
      text.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> getWidth() * .95, widthProperty()));
      text.getStyleClass().addAll(cssClasses);
      text.setStyle("-fx-fill: -fx-text-color;");
      setGraphic(text);
    }
  }
}
