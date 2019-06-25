package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Party {

  private ObjectProperty<Player> owner;
  private ObservableList<Player> members;
  private ObservableList<Player> readyMembers;

  public Party() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
    readyMembers = FXCollections.observableArrayList();
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
