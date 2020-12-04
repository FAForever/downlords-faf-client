package com.faforever.client.teammatchmaking;

import com.faforever.client.game.Faction;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class Party {

  private final ObjectProperty<Player> owner;
  private ObservableList<PartyMember> members;

  public Party() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
  }

  public void fromInfoMessage(PartyInfoMessage message, PlayerService playerService) {
    playerService.getPlayersByIds(List.of(message.getOwner()))
        .thenAccept(players -> {
          if (!players.isEmpty()) {
            Platform.runLater(() -> owner.set(players.get(0)));
          }
        });

    playerService
        .getPlayersByIds(message.getMembers().stream()
            .map(PartyInfoMessage.PartyMember::getPlayer)
            .collect(Collectors.toList()))
        .thenAccept(players -> {
          List<PartyMember> members = message.getMembers().stream()
              .map(member -> {
                Optional<Player> player;
                if (playerService.getCurrentPlayer()
                    .map(Player::getId)
                    .map(id -> id.equals(member.getPlayer()))
                    .orElse(false)) {
                  player = playerService.getCurrentPlayer(); // The player found by the search below might contain less information (e.g. a missing flag)
                } else {
                  player = players.stream()
                      .filter(playerToBeFiltered -> playerToBeFiltered.getId() == member.getPlayer())
                      .findFirst();
                }

                if (player.isEmpty()) {
                  log.warn("Could not find party member {}", member.getPlayer());
                  return null;
                } else {
                  return new PartyMember(player.get(), member.getFactions());
                }
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
          //TODO: this is a race condition. The api might answer with big delay and then server message order might be changed.
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

  public void setMembers(ObservableList<PartyMember> members) {
    this.members = members;
  }

  @Data
  @AllArgsConstructor
  public static class PartyMember {
    private final Player player;
    private List<Faction> factions;

    public PartyMember(Player player) {
      this.player = player;
      this.factions = Arrays.asList(Faction.AEON, Faction.CYBRAN, Faction.SERAPHIM, Faction.UEF);
    }
  }
}
