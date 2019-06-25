package com.faforever.client.teammatchmaking;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.remote.domain.PartyInfoMessage;
import com.faforever.client.remote.domain.PartyInviteMessage;
import com.faforever.client.remote.domain.PartyKickedMessage;
import com.faforever.client.util.IdenticonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Lazy
@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final I18n i18n;

  @Getter
  private Party party;


  public TeamMatchmakingService(FafServerAccessor fafServerAccessor, PlayerService playerService, NotificationService notificationService, I18n i18n) {
    this.fafServerAccessor = fafServerAccessor;
    this.playerService = playerService;
    this.notificationService = notificationService;
    this.i18n = i18n;

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
    playerService.getPlayersByIds(Collections.singletonList(message.getSender()))
        .thenAccept(players -> {
          if (players.isEmpty()) {
            log.error("User {} of party invite not found", message.getSender());
            return;
          }
          Player player = players.get(0);
          ActionCallback callback = event -> fafServerAccessor.acceptPartyInvite(player);

          notificationService.addNotification(new TransientNotification(
              i18n.get("teammatchmaking.notification.invite.title"),
              i18n.get("teammatchmaking.notification.invite.message", player.getUsername()),
              IdenticonUtil.createIdenticon(player.getId()),
              callback
          ));

          notificationService.addNotification(new PersistentNotification(
              i18n.get("teammatchmaking.notification.invite.message", player.getUsername()),
              Severity.INFO,
              Collections.singletonList(new Action(
                  i18n.get("teammatchmaking.notification.invite.join"),
                  callback
              ))
          ));
        });
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
