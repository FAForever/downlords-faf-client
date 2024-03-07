package com.faforever.client.builders;

import com.faforever.client.domain.server.PartyInfo;
import com.faforever.client.domain.server.PartyInfo.PartyMember;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;

import java.util.List;

public class PartyInfoBuilder {
  private final PartyInfo party = new PartyInfo();

  public static PartyInfoBuilder create() {
    return new PartyInfoBuilder();
  }

  public PartyInfoBuilder defaultValues() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    owner(player);
    members(List.of(PartyMemberBuilder.create(player).defaultValues().get()));
    return this;
  }

  public PartyInfoBuilder owner(PlayerInfo owner) {
    party.setOwner(owner);
    return this;
  }

  public PartyInfoBuilder members(List<PartyMember> members) {
    party.setMembers(members);
    return this;
  }

  public PartyInfo get() {
    return party;
  }


  public static class PartyMemberBuilder {
    private final PartyMember partyMember;

    public PartyMemberBuilder(PlayerInfo player) {
      partyMember = new PartyMember(player);
    }

    public static PartyMemberBuilder create(PlayerInfo player) {
      return new PartyMemberBuilder(player);
    }

    public PartyMemberBuilder defaultValues() {
      factions(List.of(Faction.values()));
      gameStatusChangeListener(null);
      return this;
    }

    public PartyMemberBuilder factions(List<Faction> factions) {
      partyMember.setFactions(factions);
      return this;
    }

    public PartyMemberBuilder gameStatusChangeListener(InvalidationListener gameStatusChangeListener) {
      partyMember.setGameStatusChangeListener(gameStatusChangeListener);
      return this;
    }

    public PartyMember get() {
      return partyMember;
    }

  }
}
