package com.faforever.client.ui.converter;

import com.faforever.client.ui.dialog.Dialog.DialogTransition;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

public class DialogTransitionConverter extends StyleConverter<String, DialogTransition> {
  private static final DialogTransitionConverter INSTANCE = new DialogTransitionConverter();

  public static StyleConverter<String, DialogTransition> getInstance() {
    return INSTANCE;
  }

  private DialogTransitionConverter() {
  }

  @Override
  public DialogTransition convert(ParsedValue<String, DialogTransition> value, Font unused) {
    String string = value.getValue();
    try {
      return DialogTransition.valueOf(string);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return DialogTransition.CENTER;
    }
  }

  @Override
  public String toString() {
    return "DialogTransitionConverter";
  }
}
