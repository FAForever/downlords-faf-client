package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;

import java.util.Objects;
import java.util.function.Function;

public class StringListCell<T> extends ListCell<T> {

  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final Function<T, Node> graphicFunction;
  private final Function<T, String> function;
  private final Pos alignment;

  public StringListCell(Function<T, String> function, FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    this(function, null, fxApplicationThreadExecutor);
  }

  public StringListCell(Function<T, String> function, Function<T, Node> graphicFunction,
                        FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    this(fxApplicationThreadExecutor, function, graphicFunction, Pos.CENTER_LEFT);
  }

  public StringListCell(FxApplicationThreadExecutor fxApplicationThreadExecutor, Function<T, String> function,
                        Function<T, Node> graphicFunction, Pos alignment, String... cssClasses) {
    this.fxApplicationThreadExecutor = fxApplicationThreadExecutor;
    this.function = function;
    this.alignment = alignment;
    this.graphicFunction = graphicFunction;
    getStyleClass().addAll(cssClasses);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    fxApplicationThreadExecutor.execute(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        if (graphicFunction != null) {
          setGraphic(graphicFunction.apply(item));
        }
        setText(Objects.toString(function.apply(item), ""));
        setAlignment(alignment);
      }
    });
  }
}
