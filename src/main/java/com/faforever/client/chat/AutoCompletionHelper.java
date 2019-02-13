package com.faforever.client.chat;

import com.faforever.client.player.PlayerService;
import com.faforever.client.util.Assert;
import javafx.event.EventHandler;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AutoCompletionHelper {

  private final PlayerService playerService;

  private List<String> possibleAutoCompletions;
  @Setter
  private Set<String> channelUserNames;
  private int nextAutoCompleteIndex;
  private String autoCompletePartialName;

  private TextInputControl boundTextField;
  private EventHandler<KeyEvent> keyEventHandler;

  @Inject
  public AutoCompletionHelper(PlayerService playerService) {
    this.playerService = playerService;

    keyEventHandler = keyEvent -> {
      if (!keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.TAB) {
        keyEvent.consume();
        autoComplete();
      }
    };
  }

  private void autoComplete() {
    if (boundTextField.getText().isEmpty()) {
      return;
    }

    if (possibleAutoCompletions == null) {
      initializeAutoCompletion();

      if (possibleAutoCompletions.isEmpty()) {
        // There are no auto completion matches
        resetAutoCompletion();
        return;
      }

      // It's the first auto complete event at this location, just replace the text with the first user name
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

    Set<String> playerNames = playerService.getPlayerNames();

    if (channelUserNames != null) {
      playerNames.addAll(channelUserNames);
    }

    possibleAutoCompletions.addAll(
        playerNames.stream()
            .filter(playerName -> playerName.toLowerCase(US).startsWith(autoCompletePartialName.toLowerCase()))
            .sorted()
            .collect(Collectors.toList())
    );
  }

  public void bindTo(TextInputControl messageTextField) {
    Assert.checkNotNullIllegalState(boundTextField, "AutoCompletionHelper is already bound to a TextInputControl");
    boundTextField = messageTextField;
    boundTextField.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
  }

  public void unbind() {
    if (boundTextField != null) {
      boundTextField.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
      boundTextField = null;
    }
  }

  public boolean isBound() {
    return boundTextField != null;
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
