package com.faforever.client.player;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class PlayerNoteController implements Controller<VBox> {

  private final PlayerService playerService;

  public VBox root;
  public TextArea textArea;
  public Label charactersCountLabel;
  public Button cancelButton;
  public Button okButton;

  private final int maxSymbolLimit = 150;
  TextFormatter<String> formatter = new TextFormatter<>(change ->
      isSymbolLimitReached(change) || isMultipleSeparators(change) ? null : change);

  private PlayerBean player;

  @Override
  public void initialize() {
    textArea.setTextFormatter(formatter);
    JavaFxUtil.addListener(textArea.lengthProperty(), observable ->
        charactersCountLabel.setText(String.format("%d / %d", textArea.getLength(), maxSymbolLimit)));
  }

  private boolean isSymbolLimitReached(Change change) {
    return change.getControlNewText().length() > maxSymbolLimit;
  }

  private boolean isMultipleSeparators(Change change) {
    return change.getControlText().endsWith("\n") && change.getControlNewText().endsWith("\n");
  }

  public void setPlayer(PlayerBean player) {
    this.player = player;
    this.textArea.setText(player.getNote());
  }

  public void okButtonClicked() {
    playerService.updateNote(player, textArea.getText());
    cancelButton.fire();
  }

  public void setCloseButtonAction(EventHandler<ActionEvent> handler) {
    cancelButton.setOnAction(handler);
  }

  public void requestFocus() {
    textArea.positionCaret(textArea.getLength());
    textArea.requestFocus();
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
