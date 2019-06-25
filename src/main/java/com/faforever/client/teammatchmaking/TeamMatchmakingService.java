package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.remote.domain.PartyInfoMessage;
import com.faforever.client.remote.domain.PartyInviteMessage;
import com.faforever.client.remote.domain.PartyKickedMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final PlayerService playerService;

  @Getter
  private Party party;


  public TeamMatchmakingService(FafServerAccessor fafServerAccessor, PlayerService playerService) {
    this.fafServerAccessor = fafServerAccessor;
    this.playerService = playerService;

    fafServerAccessor.addOnMessageListener(PartyInviteMessage.class, this::onPartyInvite);
    fafServerAccessor.addOnMessageListener(PartyKickedMessage.class, this::onPartyKicked);
    fafServerAccessor.addOnMessageListener(PartyInfoMessage.class, this::onPartyInfo);

    party = new Party();

    playerService.currentPlayerProperty().addListener((obs, old, player) -> {
      if (party.getOwner() == null && party.getMembers().isEmpty() && player != null) {
        party.setOwner(player);
        party.getMembers().add(player);
      }
    });
  }

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  public void onPartyInvite(PartyInviteMessage message) {
    //TODO:
  }

  public void onPartyKicked(PartyKickedMessage message) {
    //TODO:
  }

  public void onPartyInfo(PartyInfoMessage message) {
    //TODO:
  }

  public void invitePlayer(String player) {
    playerService.getPlayerForUsername(player).ifPresent(fafServerAccessor::inviteToParty);
  }

  public void kickPlayerFromParty(Player player) {
    //todo
  }
}
