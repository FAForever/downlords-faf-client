package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.SearchModMessage;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.FactionTypeAdapter;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.ModTypeTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.io.QDataWriter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.google.gson.GsonBuilder;
import javafx.beans.property.StringProperty;

import java.io.IOException;

public class ClientMessageSerializer extends JsonMessageSerializer<ClientMessage> {

  private String username;
  private StringProperty sessionIdProperty;

  /**
   * Creates a message serializer that does not append username and session ID to sent messages.
   */
  public ClientMessageSerializer() {

  }

  /**
   * Creates a message serializer that appends username and session ID to sent messages.
   *
   * @param sessionIdProperty the session ID property, so that this serializer can be initialized before the session ID
   * has been set, but it will still get it afterwards.
   */
  public ClientMessageSerializer(String username, StringProperty sessionIdProperty) {
    this.username = username;
    this.sessionIdProperty = sessionIdProperty;
  }

  @Override
  protected void appendMore(QDataWriter qDataWriter) throws IOException {
    if (username != null) {
      qDataWriter.append(username);
    }
    if (sessionIdProperty != null) {
      qDataWriter.append(sessionIdProperty.get());
    }
  }

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameState.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
        .registerTypeAdapter(SearchModMessage.ModType.class, ModTypeTypeAdapter.INSTANCE);
  }
}
