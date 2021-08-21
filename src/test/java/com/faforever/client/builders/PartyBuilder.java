package com.faforever.client.builders;

import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.commons.lobby.Faction;
import javafx.beans.InvalidationListener;

import java.util.List;

public class PartyBuilder {
  private final PartyBean party = new PartyBean();

  public static PartyBuilder create() {
    return new PartyBuilder();
  }

  public PartyBuilder defaultValues() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    owner(player);
    members(List.of(PartyMemberBuilder.create(player).defaultValues().get()));
    return this;
  }

  public PartyBuilder owner(PlayerBean owner) {
    party.setOwner(owner);
    return this;
  }

  public PartyBuilder members(List<PartyMember> members) {
    party.setMembers(members);
    return this;
  }

  public PartyBean get() {
    return party;
  }


  public static class PartyMemberBuilder {
    private final PartyMember partyMember;

    public PartyMemberBuilder(PlayerBean player) {
      partyMember = new PartyMember(player);
    }

    public static PartyMemberBuilder create(PlayerBean player) {
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
