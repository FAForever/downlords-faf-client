package com.faforever.client.remote;

import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameVisibility;
import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.ConnectivityStateTypeAdapter;
import com.faforever.client.remote.gson.FactionTypeAdapter;
import com.faforever.client.remote.gson.GameAccessTypeAdapter;
import com.faforever.client.remote.gson.GameStateTypeAdapter;
import com.faforever.client.remote.gson.GameVisibilityTypeAdapter;
import com.faforever.client.remote.gson.InetSocketAddressTypeAdapter;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter;
import com.google.gson.GsonBuilder;

import java.net.InetSocketAddress;

public class ClientMessageSerializer extends JsonMessageSerializer<ClientMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameStatus.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameVisibility.class, GameVisibilityTypeAdapter.INSTANCE)
        .registerTypeAdapter(ConnectivityState.class, ConnectivityStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .registerTypeAdapter(InetSocketAddress.class, InetSocketAddressTypeAdapter.INSTANCE);
  }
}
