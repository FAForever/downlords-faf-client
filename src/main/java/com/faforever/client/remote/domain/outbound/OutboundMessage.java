package com.faforever.client.remote.domain.outbound;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.inbound.gpg.DisconnectFromPeerMessage;
import com.faforever.client.remote.domain.outbound.faf.AcceptPartyInviteMessage;
import com.faforever.client.remote.domain.outbound.faf.AddFoeMessage;
import com.faforever.client.remote.domain.outbound.faf.AddFriendMessage;
import com.faforever.client.remote.domain.outbound.faf.AddSocialMessage;
import com.faforever.client.remote.domain.outbound.faf.AdminMessage;
import com.faforever.client.remote.domain.outbound.faf.AvatarMessage;
import com.faforever.client.remote.domain.outbound.faf.BanPlayerMessage;
import com.faforever.client.remote.domain.outbound.faf.ClosePlayersFAMessage;
import com.faforever.client.remote.domain.outbound.faf.ClosePlayersLobbyMessage;
import com.faforever.client.remote.domain.outbound.faf.GameMatchmakingMessage;
import com.faforever.client.remote.domain.outbound.faf.HostGameMessage;
import com.faforever.client.remote.domain.outbound.faf.InitSessionMessage;
import com.faforever.client.remote.domain.outbound.faf.InviteToPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.JoinGameMessage;
import com.faforever.client.remote.domain.outbound.faf.KickPlayerFromPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.LeavePartyMessage;
import com.faforever.client.remote.domain.outbound.faf.ListIceServersMessage;
import com.faforever.client.remote.domain.outbound.faf.ListPersonalAvatarsMessage;
import com.faforever.client.remote.domain.outbound.faf.LoginOauthClientMessage;
import com.faforever.client.remote.domain.outbound.faf.MakeBroadcastMessage;
import com.faforever.client.remote.domain.outbound.faf.MatchReadyMessage;
import com.faforever.client.remote.domain.outbound.faf.MatchmakerInfoOutboundMessage;
import com.faforever.client.remote.domain.outbound.faf.PingMessage;
import com.faforever.client.remote.domain.outbound.faf.PongMessage;
import com.faforever.client.remote.domain.outbound.faf.ReadyPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.RemoveFoeMessage;
import com.faforever.client.remote.domain.outbound.faf.RemoveFriendMessage;
import com.faforever.client.remote.domain.outbound.faf.RemoveSocialMessage;
import com.faforever.client.remote.domain.outbound.faf.RestoreGameSessionMessage;
import com.faforever.client.remote.domain.outbound.faf.SelectAvatarMessage;
import com.faforever.client.remote.domain.outbound.faf.SetPartyFactionsMessage;
import com.faforever.client.remote.domain.outbound.faf.UnreadyPartyMessage;
import com.faforever.client.remote.domain.outbound.gpg.AIOptionMessage;
import com.faforever.client.remote.domain.outbound.gpg.ChatMessage;
import com.faforever.client.remote.domain.outbound.gpg.ClearSlotMessage;
import com.faforever.client.remote.domain.outbound.gpg.ConnectedMessage;
import com.faforever.client.remote.domain.outbound.gpg.ConnectedToHostMessage;
import com.faforever.client.remote.domain.outbound.gpg.DesyncMessage;
import com.faforever.client.remote.domain.outbound.gpg.DisconnectedMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameEndedMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameFullMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameModsMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameOptionMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameResultMessage;
import com.faforever.client.remote.domain.outbound.gpg.GameStateMessage;
import com.faforever.client.remote.domain.outbound.gpg.IceMessage;
import com.faforever.client.remote.domain.outbound.gpg.JsonStatsMessage;
import com.faforever.client.remote.domain.outbound.gpg.PlayerOptionMessage;
import com.faforever.client.remote.domain.outbound.gpg.RehostMessage;
import com.faforever.client.remote.domain.outbound.gpg.StatsMessage;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@Data
@JsonTypeInfo(
    use = Id.NAME,
    property = "command",
    visible = true
)
@JsonSubTypes({
    //FAF Client Messages
    @Type(value = HostGameMessage.class, name = HostGameMessage.COMMAND),
    @Type(value = JoinGameMessage.class, name = JoinGameMessage.COMMAND),
    @Type(value = InitSessionMessage.class, name = InitSessionMessage.COMMAND),
    @Type(value = AddFriendMessage.class, name = AddSocialMessage.COMMAND),
    @Type(value = AddFoeMessage.class, name = AddSocialMessage.COMMAND),
    @Type(value = RemoveFriendMessage.class, name = RemoveSocialMessage.COMMAND),
    @Type(value = RemoveFoeMessage.class, name = RemoveSocialMessage.COMMAND),
    @Type(value = ListPersonalAvatarsMessage.class, name = AvatarMessage.COMMAND),
    @Type(value = SelectAvatarMessage.class, name = AvatarMessage.COMMAND),
    @Type(value = ListIceServersMessage.class, name = ListIceServersMessage.COMMAND),
    @Type(value = RestoreGameSessionMessage.class, name = RestoreGameSessionMessage.COMMAND),
    @Type(value = PingMessage.class, name = PingMessage.COMMAND),
    @Type(value = PongMessage.class, name = PongMessage.COMMAND),
    @Type(value = ClosePlayersFAMessage.class, name = AdminMessage.COMMAND),
    @Type(value = BanPlayerMessage.class, name = AdminMessage.COMMAND),
    @Type(value = ClosePlayersLobbyMessage.class, name = AdminMessage.COMMAND),
    @Type(value = MakeBroadcastMessage.class, name = AdminMessage.COMMAND),
    @Type(value = InviteToPartyMessage.class, name = InviteToPartyMessage.COMMAND),
    @Type(value = AcceptPartyInviteMessage.class, name = AcceptPartyInviteMessage.COMMAND),
    @Type(value = KickPlayerFromPartyMessage.class, name = KickPlayerFromPartyMessage.COMMAND),
    @Type(value = LeavePartyMessage.class, name = LeavePartyMessage.COMMAND),
    @Type(value = ReadyPartyMessage.class, name = ReadyPartyMessage.COMMAND),
    @Type(value = UnreadyPartyMessage.class, name = UnreadyPartyMessage.COMMAND),
    @Type(value = SetPartyFactionsMessage.class, name = SetPartyFactionsMessage.COMMAND),
    @Type(value = GameMatchmakingMessage.class, name = GameMatchmakingMessage.COMMAND),
    @Type(value = MatchmakerInfoOutboundMessage.class, name = MatchmakerInfoOutboundMessage.COMMAND),
    @Type(value = MatchReadyMessage.class, name = MatchReadyMessage.COMMAND),
    @Type(value = LoginOauthClientMessage.class, name = LoginOauthClientMessage.COMMAND),
    // GPG Client Messages not directly instantiated they are only forwarded from the game
    // Listing here serves as documentation of known messages
    @Type(value = DisconnectedMessage.class, name = DisconnectedMessage.COMMAND),
    @Type(value = ConnectedMessage.class, name = ConnectedMessage.COMMAND),
    @Type(value = GameStateMessage.class, name = GameStateMessage.COMMAND),
    @Type(value = GameOptionMessage.class, name = GameOptionMessage.COMMAND),
    @Type(value = GameModsMessage.class, name = GameModsMessage.COMMAND),
    @Type(value = PlayerOptionMessage.class, name = PlayerOptionMessage.COMMAND),
    @Type(value = DisconnectFromPeerMessage.class, name = DisconnectFromPeerMessage.COMMAND),
    @Type(value = ChatMessage.class, name = ChatMessage.COMMAND),
    @Type(value = GameResultMessage.class, name = GameResultMessage.COMMAND),
    @Type(value = StatsMessage.class, name = StatsMessage.COMMAND),
    @Type(value = ClearSlotMessage.class, name = ClearSlotMessage.COMMAND),
    @Type(value = AIOptionMessage.class, name = AIOptionMessage.COMMAND),
    @Type(value = JsonStatsMessage.class, name = JsonStatsMessage.COMMAND),
    @Type(value = RehostMessage.class, name = RehostMessage.COMMAND),
    @Type(value = DesyncMessage.class, name = DesyncMessage.COMMAND),
    @Type(value = GameFullMessage.class, name = GameFullMessage.COMMAND),
    @Type(value = GameEndedMessage.class, name = GameEndedMessage.COMMAND),
    @Type(value = IceMessage.class, name = IceMessage.COMMAND),
    @Type(value = ConnectedToHostMessage.class, name = ConnectedToHostMessage.COMMAND)
})
public abstract class OutboundMessage implements SerializableMessage {

  private final MessageTarget target;

  @Override
  public Collection<String> getStringsToMask() {
    return List.of();
  }
}
