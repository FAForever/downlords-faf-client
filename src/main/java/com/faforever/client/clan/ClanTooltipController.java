package com.faforever.client.clan;


import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
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
  public Label descriptionTitleLabel;
  private final I18n i18n;

  public void initialize() {
    descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
    descriptionTitleLabel.managedProperty().bind(descriptionTitleLabel.visibleProperty());
    descriptionLabel.visibleProperty().bind(descriptionLabel.textProperty().isNotEmpty());
    descriptionTitleLabel.visibleProperty().bind(descriptionLabel.textProperty().isNotEmpty());
  }

  public ClanTooltipController(I18n i18n) {
    this.i18n = i18n;
  }

  public void setClan(Clan clan) {
    nameLabel.setText(clan.getName());
    // TODO improve formatting
    membersLabel.setText(i18n.number(clan.getMembers().size()));
    descriptionLabel.setText(clan.getDescription());
    leaderLabel.setText(clan.getLeader().getUsername());
    descriptionLabel.setText(Optional.ofNullable(clan.getDescription()).orElse(""));
  }

  public Pane getRoot() {
    return clanTooltipRoot;
  }
}
