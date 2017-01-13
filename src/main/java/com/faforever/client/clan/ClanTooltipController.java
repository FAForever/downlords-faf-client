package com.faforever.client.clan;


import com.faforever.client.fx.Controller;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClanTooltipController implements Controller<Node> {

  public Pane clanTooltipRoot;
  public Label nameLabel;
  public Label descriptionLabel;
  public Label membersLabel;
  public Label leaderLabel;

  public void initialize() {
    descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
    descriptionLabel.visibleProperty().bind(descriptionLabel.textProperty().isNotEmpty());
  }

  public void setClan(Clan clan) {
    nameLabel.setText(clan.getName());
    // TODO improve formatting
    membersLabel.setText(clan.getMembers().toString());
    descriptionLabel.setText(clan.getDescription());
    leaderLabel.setText(clan.getLeader().getUsername());
    descriptionLabel.setText(Optional.ofNullable(clan.getDescription()).orElse(""));
  }

  public Pane getRoot() {
    return clanTooltipRoot;
  }
}
