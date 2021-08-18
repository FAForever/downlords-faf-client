package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;

import java.util.List;

public class PartyBuilder {
  private final Party party = new Party();

  public static PartyBuilder create() {
    return new PartyBuilder();
  }

  public PartyBuilder defaultValues() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    owner(player);
    members(List.of(PartyMemberBuilder.create(player).defaultValues().get()));
    return this;
  }

  public PartyBuilder owner(Player owner) {
    party.setOwner(owner);
    return this;
  }

  public PartyBuilder members(List<PartyMember> members) {
    party.setMembers(members);
    return this;
  }

  public Party get() {
    return party;
  }


  public static class PartyMemberBuilder {
    private final PartyMember partyMember;

    public PartyMemberBuilder(Player player) {
      partyMember = new PartyMember(player);
    }

    public static PartyMemberBuilder create(Player player) {
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
