package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.ConnectToPeerMessage;
import com.faforever.client.fa.relay.DisconnectFromPeerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;
import com.faforever.client.fa.relay.HostGameMessage;
import com.faforever.client.fa.relay.JoinGameMessage;
import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.game.Faction;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.IceServerMessage;
import com.faforever.client.remote.domain.IceServersServerMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MatchCancelledMessage;
import com.faforever.client.remote.domain.MatchFoundMessage;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.remote.domain.PartyInfoMessage;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.SearchInfoMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.remote.domain.SessionMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.IceServerListResponse;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.LoginFailedResponse;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerInfo;
import com.faforever.commons.lobby.MatchmakerMatchCancelledResponse;
import com.faforever.commons.lobby.MatchmakerMatchFoundResponse;
import com.faforever.commons.lobby.NoticeInfo;
import com.faforever.commons.lobby.PartyInfo;
import com.faforever.commons.lobby.PlayerInfo;
import com.faforever.commons.lobby.SearchInfo;
import com.faforever.commons.lobby.SessionResponse;
import com.faforever.commons.lobby.SocialInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServerMessageMapper {
  private final ObjectMapper objectMapper;

  private final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
      .registerTypeAdapter(GameStatus.class, GameStateTypeAdapter.INSTANCE)
      .registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
      .registerTypeAdapter(GameType.class, GameTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
      .registerTypeAdapter(ServerMessage.class, ServerMessageTypeAdapter.INSTANCE)
      .registerTypeAdapter(RatingRange.class, RatingRangeTypeAdapter.INSTANCE)
      .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
      .registerTypeAdapter(LobbyMode.class, LobbyModeTypeAdapter.INSTANCE)
      .registerTypeAdapter(MatchmakingState.class, MatchmakingStateTypeAdapter.INSTANCE)
      .create();

  Map<Class<? extends com.faforever.commons.lobby.ServerMessage>, Class<? extends ServerMessage>> toGsonMapping
      = ImmutableMap.<Class<? extends com.faforever.commons.lobby.ServerMessage>, Class<? extends ServerMessage>>builder()
      .put(LoginFailedResponse.class, AuthenticationFailedMessage.class)
      .put(SessionResponse.class, SessionMessage.class)
      .put(NoticeInfo.class, NoticeMessage.class)
      .put(LoginSuccessResponse.class, LoginMessage.class)
      .put(PlayerInfo.class, PlayersMessage.class)
      .put(SocialInfo.class, SocialMessage.class)
      .put(MatchmakerInfo.class, MatchmakerInfoMessage.class)
      .put(PartyInfo.class, PartyInfoMessage.class)
      .put(GameInfo.class, GameInfoMessage.class)
      .put(GameLaunchResponse.class, GameLaunchMessage.class)
      .put(MatchmakerMatchFoundResponse.class, MatchFoundMessage.class)
      .put(MatchmakerMatchCancelledResponse.class, MatchCancelledMessage.class)
      .put(SearchInfo.class, SearchInfoMessage.class)
      .put(IceServerListResponse.class, IceServersServerMessage.class)
      .put(HostGameGpgCommand.class, HostGameMessage.class)
      .put(JoinGameGpgCommand.class, JoinGameMessage.class)
      .put(ConnectToPeerGpgCommand.class, ConnectToPeerMessage.class)
      .put(IceMsgGpgCommand.class, IceServerMessage.class)
      .put(DisconnectFromPeerGpgCommand.class, DisconnectFromPeerMessage.class)
      .build();

  @SneakyThrows
  public <T extends ServerMessage> T jackson2Gson(com.faforever.commons.lobby.ServerMessage jacksonMessage) {
    String jsonString = objectMapper.writeValueAsString(jacksonMessage);
    return (T) gson.fromJson(jsonString, toGsonMapping.get(jacksonMessage.getClass()));
  }

  @SneakyThrows
  public <T extends com.faforever.commons.lobby.ServerMessage> T gson2Jackson(ServerMessage gsonMessage) {
    String jsonString = gson.toJson(gsonMessage);
    return (T) objectMapper.readValue(jsonString, com.faforever.commons.lobby.ServerMessage.class);
  }

}
