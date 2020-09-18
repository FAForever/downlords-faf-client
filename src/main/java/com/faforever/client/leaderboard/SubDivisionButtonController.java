package com.faforever.client.leaderboard;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)

public class SubDivisionButtonController implements Controller<Node> {

  public ToggleButton subDivisionToggleButton;
  public  ToggleGroup group;
  @Getter
  @Setter
  private Division division;

  @Override
  public Node getRoot() {
    return subDivisionToggleButton;
  }

  public void setButtonText(String text) {
    subDivisionToggleButton.setText(text);
  }

  public void setButtonSelected(boolean selected) {
    subDivisionToggleButton.setSelected(selected);
  }

  public void setToggleGroup(ToggleGroup toggleGroup) {
    subDivisionToggleButton.setToggleGroup(toggleGroup);
  }

  public void onSubDivisionButtonPressed(ActionEvent actionEvent) {

  }
}
