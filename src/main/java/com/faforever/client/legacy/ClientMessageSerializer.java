package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameVisibility;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.SearchModMessage;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.ConnectivityStateTypeAdapter;
import com.faforever.client.legacy.gson.FactionTypeAdapter;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GameVisibilityTypeAdapter;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ModTypeTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.io.QDataWriter;
import com.faforever.client.legacy.writer.JsonMessageSerializer;
import com.faforever.client.portcheck.ConnectivityState;
import com.google.gson.GsonBuilder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class ClientMessageSerializer extends JsonMessageSerializer<ClientMessage> {

  private StringProperty username;
  private ObjectProperty<Long> sessionId;

  /**
   * Creates a message serializer that does not append username and session ID to sent messages.
   */
  public ClientMessageSerializer() {
    this(new SimpleStringProperty(), new SimpleObjectProperty<>());
  }

  /**
   * Creates a message serializer that appends username and session ID to sent messages.
   *
   * @param username the username property, so that this serializer can be initialized before the session ID
   * @param sessionId the session ID property, so that this serializer can be initialized before the session ID
   */
  public ClientMessageSerializer(StringProperty username, ObjectProperty<Long> sessionId) {
    this.username = username;
    this.sessionId = sessionId;
  }

  @Override
  protected void appendMore(QDataWriter qDataWriter) throws IOException {
    qDataWriter.append(StringUtils.defaultString(username.get()));
    if (sessionId.get() == null) {
      qDataWriter.append("");
    } else {
      qDataWriter.append(sessionId.get().toString());
    }
  }

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameState.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameVisibility.class, GameVisibilityTypeAdapter.INSTANCE)
        .registerTypeAdapter(ConnectivityState.class, ConnectivityStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .registerTypeAdapter(SearchModMessage.ModType.class, ModTypeTypeAdapter.INSTANCE);
  }
}
