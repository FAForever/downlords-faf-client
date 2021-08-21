package com.faforever.client.remote.domain.inbound;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.inbound.faf.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.inbound.faf.AvatarMessage;
import com.faforever.client.remote.domain.inbound.faf.GameInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage;
import com.faforever.client.remote.domain.inbound.faf.IrcPasswordServerMessage;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchCancelledMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchFoundMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.NoticeMessage;
import com.faforever.client.remote.domain.inbound.faf.PartyInviteMessage;
import com.faforever.client.remote.domain.inbound.faf.PartyKickedMessage;
import com.faforever.client.remote.domain.inbound.faf.PlayerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.SearchInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.SessionMessage;
import com.faforever.client.remote.domain.inbound.faf.SocialMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatePartyMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatedAchievementsMessage;
import com.faforever.client.remote.domain.inbound.gpg.ConnectToPeerMessage;
import com.faforever.client.remote.domain.inbound.gpg.DisconnectFromPeerMessage;
import com.faforever.client.remote.domain.inbound.gpg.GpgHostGameMessage;
import com.faforever.client.remote.domain.inbound.gpg.GpgJoinGameMessage;
import com.faforever.client.remote.domain.inbound.gpg.IceInboundMessage;
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
    property = "command"
)
@JsonSubTypes({
    //FAF Server Messages
    @Type(value = LoginMessage.class, name = LoginMessage.COMMAND),
    @Type(value = SessionMessage.class, name = SessionMessage.COMMAND),
    @Type(value = GameInfoMessage.class, name = GameInfoMessage.COMMAND),
    @Type(value = PlayerInfoMessage.class, name = PlayerInfoMessage.COMMAND),
    @Type(value = GameLaunchMessage.class, name = GameLaunchMessage.COMMAND),
    @Type(value = MatchmakerInfoMessage.class, name = MatchmakerInfoMessage.COMMAND),
    @Type(value = MatchFoundMessage.class, name = MatchFoundMessage.COMMAND),
    @Type(value = MatchCancelledMessage.class, name = MatchCancelledMessage.COMMAND),
    @Type(value = MatchInfoMessage.class, name = MatchInfoMessage.COMMAND),
    @Type(value = SocialMessage.class, name = SocialMessage.COMMAND),
    @Type(value = AuthenticationFailedMessage.class, name = AuthenticationFailedMessage.COMMAND),
    @Type(value = UpdatedAchievementsMessage.class, name = UpdatedAchievementsMessage.COMMAND),
    @Type(value = NoticeMessage.class, name = NoticeMessage.COMMAND),
    @Type(value = IceServersMessage.class, name = IceServersMessage.COMMAND),
    @Type(value = AvatarMessage.class, name = AvatarMessage.COMMAND),
    @Type(value = UpdatePartyMessage.class, name = UpdatePartyMessage.COMMAND),
    @Type(value = PartyInviteMessage.class, name = PartyInviteMessage.COMMAND),
    @Type(value = PartyKickedMessage.class, name = PartyKickedMessage.COMMAND),
    @Type(value = SearchInfoMessage.class, name = SearchInfoMessage.COMMAND),
    @Type(value = IrcPasswordServerMessage.class, name = IrcPasswordServerMessage.COMMAND),
    //GPG Server messages
    @Type(value = GpgHostGameMessage.class, name = GpgHostGameMessage.COMMAND),
    @Type(value = GpgJoinGameMessage.class, name = GpgJoinGameMessage.COMMAND),
    @Type(value = ConnectToPeerMessage.class, name = ConnectToPeerMessage.COMMAND),
    @Type(value = IceInboundMessage.class, name = IceInboundMessage.COMMAND),
    @Type(value = DisconnectFromPeerMessage.class, name = DisconnectFromPeerMessage.COMMAND)
})
public abstract class InboundMessage implements SerializableMessage {

  private final MessageTarget target;

  @Override
  public Collection<String> getStringsToMask() {
    return List.of();
  }
}
