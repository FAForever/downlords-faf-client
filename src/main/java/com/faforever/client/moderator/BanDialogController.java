package com.faforever.client.moderator;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.ui.dialog.DialogLayout;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BanDialogController implements Controller<Node> {

  private final ModeratorService moderatorService;
  private final I18n i18n;
  public Pane banDialogRoot;
  public Button dismissButton;
  public Label userLabel;
  public TextField reasonText;
  public TextField durationText;
  public ChoiceBox<PeriodType> periodTypeChoiceBox;
  public Button banButton;
  public Label successsLabel;
  private DialogLayout dialogLayout;
  private ObjectProperty<Player> player;

  private Runnable closeListener;

  public BanDialogController(ModeratorService moderatorService, I18n i18n) {
    this.moderatorService = moderatorService;
    this.i18n = i18n;

    dialogLayout = new DialogLayout();
    player = new SimpleObjectProperty<>();
  }

  public void initialize() {
    dialogLayout.setBody(banDialogRoot);

    userLabel.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("moderator.ban.user", player.get().getUsername()), player));

    periodTypeChoiceBox.setConverter(new StringConverter<PeriodType>() {
      @Override
      public String toString(PeriodType object) {
        return object.name();
      }

      @Override
      public PeriodType fromString(String string) {
        return PeriodType.valueOf(string);
      }
    });
    periodTypeChoiceBox.setItems(FXCollections.observableArrayList(PeriodType.values()));
    JavaFxUtil.makeNumericTextField(durationText, 5);
  }

  public Region getRoot() {
    return banDialogRoot;
  }

  public BanDialogController setPlayer(Player player) {
    this.player.set(player);
    return this;
  }

  public void onBanButtonClicked() {
    if (durationText.getText().isEmpty() || periodTypeChoiceBox.getSelectionModel().getSelectedIndex() < 0) {
      return;
    }
    moderatorService.banPlayer(player.get().getId(), Integer.parseInt(durationText.getText()), (PeriodType) periodTypeChoiceBox.getSelectionModel().getSelectedItem(), reasonText.getText());
    successsLabel.setVisible(true);
    banButton.setDisable(true);
  }

  public BanDialogController setCloseListener(Runnable closeListener) {
    this.closeListener = closeListener;
    return this;
  }

  public void dismiss() {
    closeListener.run();
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }

  public DialogLayout getDialogLayout() {
    return dialogLayout;
  }
}
