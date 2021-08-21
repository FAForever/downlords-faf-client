package com.faforever.client.domain;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PartyBean {

  @EqualsAndHashCode.Include
  private final ObjectProperty<PlayerBean> owner;
  private final ObservableList<PartyMember> members;

  public PartyBean() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
  }

  public PlayerBean getOwner() {
    return owner.get();
  }

  public void setOwner(PlayerBean owner) {
    this.owner.set(owner);
  }

  public ObjectProperty<PlayerBean> ownerProperty() {
    return owner;
  }

  public ObservableList<PartyMember> getMembers() {
    return members;
  }

  public void setMembers(List<PartyMember> members) {
    this.members.setAll(members);
  }

  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  @Data
  public static class PartyMember {
    @EqualsAndHashCode.Include
    private final PlayerBean player;
    private List<Faction> factions;
    private InvalidationListener gameStatusChangeListener;

    public PartyMember(PlayerBean player) {
      this.player = player;
      this.factions = Arrays.asList(Faction.AEON, Faction.CYBRAN, Faction.SERAPHIM, Faction.UEF);
    }

    public PartyMember(PlayerBean player, List<Faction> factions) {
      this.player = player;
      this.factions = factions;
    }

    public void setGameStatusChangeListener(InvalidationListener listener) {
      if (gameStatusChangeListener != null) {
        JavaFxUtil.removeListener(player.statusProperty(), gameStatusChangeListener);
      }
      gameStatusChangeListener = listener;
      if (gameStatusChangeListener != null) {
        JavaFxUtil.addAndTriggerListener(player.statusProperty(), gameStatusChangeListener);
      }
    }
  }
}
