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
    setOwnerFromMessage(message, playerService);
    setMembersFromMessage(message, playerService);
  }

  private void setOwnerFromMessage(PartyInfoMessage message, PlayerService playerService) {
    List<Player> players =  playerService.getOnlinePlayersByIds(List.of(message.getOwner()));
    if (!players.isEmpty()) {
      Platform.runLater(() -> owner.set(players.get(0)));
    }
  }

  private void setMembersFromMessage(PartyInfoMessage message, PlayerService playerService) {
    List<Player> players = playerService.getOnlinePlayersByIds(
        message.getMembers()
            .stream()
        .map(PartyInfoMessage.PartyMember::getPlayer)
        .collect(Collectors.toList()));

    List<PartyMember> partyMembers = message.getMembers()
        .stream()
        .map(member -> pickRightPlayerToCreatePartyMember(players, member))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    Platform.runLater(() -> this.members.setAll(partyMembers));
  }

  private PartyMember pickRightPlayerToCreatePartyMember(List<Player> players, PartyInfoMessage.PartyMember member) {
    Optional<Player> player = players.stream()
          .filter(playerToBeFiltered -> playerToBeFiltered.getId() == member.getPlayer())
          .findFirst();

    if (player.isEmpty()) {
      log.warn("Could not find party member {}", member.getPlayer());
      return null;
    } else {
      return new PartyMember(player.get(), member.getFactions());
    }
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
