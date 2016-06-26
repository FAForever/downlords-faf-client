package com.faforever.client.chat;

import com.faforever.client.player.PlayerService;
import com.faforever.client.util.Assert;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextInputControl;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Locale.US;

public class AutoCompletitionHelper {

  @Resource
  PlayerService playerService;

  private List<String> possibleAutoCompletions;
  private int nextAutoCompleteIndex;
  private String autoCompletePartialName;

  private TextInputControl boundTextField;

  private void autoComplete() {
    if (boundTextField.getText().isEmpty()) {
      return;
    }

    if (possibleAutoCompletions == null) {
      initializeAutoCompletion();

      if (possibleAutoCompletions.isEmpty()) {
        // There are no autocompletion matches
        resetAutoCompletion();
        return;
      }

      // It's the first autocomplete event at this location, just replace the text with the first user name
      boundTextField.selectPreviousWord();
      boundTextField.replaceSelection(possibleAutoCompletions.get(nextAutoCompleteIndex++));
      return;
    }

    // At this point, it's a subsequent auto complete event
    String wordBeforeCaret = getWordBeforeCaret(boundTextField);

    /*
     * We have to check if the previous word is the one we auto completed. If not we reset and start all over again
     * as the user started auto completion on another word.
     */
    if (!wordBeforeCaret.equals(possibleAutoCompletions.get(nextAutoCompleteIndex - 1))) {
      resetAutoCompletion();
      autoComplete();
      return;
    }

    if (possibleAutoCompletions.size() == 1) {
      // No need to cycle since there was only one match
      return;
    }

    if (possibleAutoCompletions.size() <= nextAutoCompleteIndex) {
      // Start over again in order to cycle
      nextAutoCompleteIndex = 0;
    }

    boundTextField.selectPreviousWord();
    boundTextField.replaceSelection(possibleAutoCompletions.get(nextAutoCompleteIndex++));
  }

  private void initializeAutoCompletion() {
    possibleAutoCompletions = new ArrayList<>();

    autoCompletePartialName = getWordBeforeCaret(boundTextField);
    if (autoCompletePartialName.isEmpty()) {
      return;
    }

    nextAutoCompleteIndex = 0;

    possibleAutoCompletions.addAll(
        playerService.getPlayerNames().stream()
            .filter(playerName -> playerName.toLowerCase(US).startsWith(autoCompletePartialName.toLowerCase()))
            .sorted()
            .collect(Collectors.toList())
    );
  }

  public void bindTo(TextInputControl messageTextField) {
    Assert.checkNull(boundTextField, "AutoCompletitionHelper is already bound to a TextInputControl");
    boundTextField = messageTextField;
    boundTextField.textProperty().addListener(this::onBoundTextFieldChanged);
  }

  public void unbind() {
    if (boundTextField != null) {
      boundTextField.textProperty().removeListener(this::onBoundTextFieldChanged);
      boundTextField = null;
    }
  }

  public boolean isBound() {
    return boundTextField != null;
  }

  private void onBoundTextFieldChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
    autoComplete();
  }

  private String getWordBeforeCaret(TextInputControl messageTextField) {
    messageTextField.selectPreviousWord();
    String selectedText = messageTextField.getSelectedText();
    messageTextField.positionCaret(messageTextField.getAnchor());
    return selectedText;
  }

  private void resetAutoCompletion() {
    possibleAutoCompletions = null;
    nextAutoCompleteIndex = -1;
    autoCompletePartialName = null;
  }
}
