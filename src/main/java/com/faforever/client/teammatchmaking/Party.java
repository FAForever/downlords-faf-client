package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.domain.PartyInfoMessage;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class Party {

  private ObjectProperty<Player> owner;
  private ObservableList<PartyMember> members;

  public Party() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
  }

  public void fromInfoMessage(PartyInfoMessage message, PlayerService playerService) {
    playerService.getPlayersByIds(Collections.singletonList(message.getOwner()))
        .thenAccept(p -> {
          if (!p.isEmpty()) {
            Platform.runLater(() -> owner.set(p.get(0)));
          }
        });

    playerService
        .getPlayersByIds(message.getMembers().stream().map(m -> m.getPlayer()).collect(Collectors.toList()))
        .thenAccept(players -> {
//          message.getMembers().stream().filter(m -> players.stream().noneMatch(p -> p.getId() == m))
//            .forEach(m -> log.warn("Could not find party member {}", m));
          List<PartyMember> members = message.getMembers().stream().map(member -> {
            Optional<Player> player = players.stream().filter(p -> p.getId() == member.getPlayer()).findFirst();
            if (!player.isPresent()) {
              log.warn("Could not find party member {}", member.getPlayer());
              return null;
            }
            return new PartyMember(player.get(), member.getReady(), member.getFactions());
          }).filter(Objects::nonNull).collect(Collectors.toList());
          Platform.runLater(() -> this.members.setAll(members));
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

  public ObservableList<PartyMember> getMembers() {
    return members;
  }

  public void setMembers(ObservableList<Player> members) {
    this.members = members;
  }

  @Data
  @AllArgsConstructor
  public static class PartyMember {
    private final Player player;
    private boolean ready;  // no properties used as this object is recreated for each recevied party info message
    private List<Boolean> factions;

    public PartyMember(Player player) {
      this.player = player;
      this.ready = false;
      this.factions = new ArrayList<>(Arrays.asList(false, false, false, false));
    }
  }
}
