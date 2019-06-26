package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.domain.PartyInfoMessage;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class Party {

  private ObjectProperty<Player> owner;
  private ObservableList<Player> members;
  private ObservableList<Player> readyMembers;

  public Party() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
    readyMembers = FXCollections.observableArrayList();
  }

  public void fromInfoMessage(PartyInfoMessage message, PlayerService playerService) {
    playerService.getPlayersByIds(Collections.singletonList(message.getOwner()))
        .thenAccept(p -> {
          if (!p.isEmpty()) {
            owner.set(p.get(0));
          }
        });

    playerService.getPlayersByIds(message.getMembers()).thenAccept(players -> {
      message.getMembers().stream().filter(m -> players.stream().noneMatch(p -> p.getId() == m))
          .forEach(m -> log.warn("Could not find party member {}", m));
      members.setAll(players);
    });

    playerService.getPlayersByIds(message.getMembers()).thenAccept(players -> {
      members.setAll(players);
    });
  }

  public Player getOwner() {
    return owner.get();
  }

  public void setOwner(Player owner) {
    this.owner.set(owner);
  }

  public ObjectProperty<Player> ownerProperty() {
    return owner;
  }

  public ObservableList<Player> getMembers() {
    return members;
  }

  public void setMembers(ObservableList<Player> members) {
    this.members = members;
  }

  public ObservableList<Player> getReadyMembers() {
    return readyMembers;
  }

  public void setReadyMembers(ObservableList<Player> readyMembers) {
    this.readyMembers = readyMembers;
  }
}
