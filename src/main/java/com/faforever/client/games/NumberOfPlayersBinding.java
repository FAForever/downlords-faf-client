package com.faforever.client.games;

import com.faforever.client.i18n.I18n;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;

public class NumberOfPlayersBinding extends StringBinding {

  private I18n i18n;
  private ObjectProperty<Integer> numPlayers;
  private ObjectProperty<Integer> maxPlayers;

  public NumberOfPlayersBinding(I18n i18n, ObjectProperty<Integer> numPlayers, ObjectProperty<Integer> maxPlayers) {
    this.i18n = i18n;
    this.numPlayers = numPlayers;
    this.maxPlayers = maxPlayers;

    bind(numPlayers, maxPlayers);
  }

  @Override
  protected String computeValue() {
    return String.format(i18n.get("game.players.format"), numPlayers.get(), maxPlayers.get());
  }
}
