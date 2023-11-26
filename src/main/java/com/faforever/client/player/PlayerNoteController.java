package com.faforever.client.player;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.social.SocialService;
import com.faforever.client.ui.dialog.Dialog;
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
public class PlayerNoteController extends NodeController<VBox> {

  public static final int CHARACTER_LIMIT = 150;

  private final SocialService socialService;

  public VBox root;
  public TextArea textArea;
  public Label charactersCountLabel;
  public Button cancelButton;
  public Button okButton;

  TextFormatter<String> formatter = new TextFormatter<>(change -> isCharacterLimitReached(change) ? null : change);

  private PlayerBean player;

  @Override
  protected void onInitialize() {
    textArea.setTextFormatter(formatter);
    JavaFxUtil.addListener(textArea.lengthProperty(), observable ->
        charactersCountLabel.setText(String.format("%d / %d", textArea.getLength(), CHARACTER_LIMIT)));
  }

  private boolean isCharacterLimitReached(Change change) {
    return change.getControlNewText().length() > CHARACTER_LIMIT;
  }

  public void setPlayer(PlayerBean player) {
    this.player = player;
    this.textArea.setText(player.getNote());
  }

  public void okButtonClicked() {
    socialService.updateNote(player, textArea.getText());
    cancelButton.fire();
  }

  public void setCloseDialogAction(Dialog dialog) {
    cancelButton.setOnAction(event -> dialog.close());
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
