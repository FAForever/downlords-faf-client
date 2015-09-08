package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;

public class NumberOfPlayersBinding extends StringBinding {

  private final I18n i18n;
  private final IntegerProperty numPlayers;
  private final IntegerProperty maxPlayers;

  public NumberOfPlayersBinding(I18n i18n, IntegerProperty numPlayers, IntegerProperty maxPlayers) {
    this.i18n = i18n;
    this.numPlayers = numPlayers;
    this.maxPlayers = maxPlayers;

    bind(numPlayers, maxPlayers);
  }

  @Override
  protected String computeValue() {
    return i18n.get("game.players.format", numPlayers.get(), maxPlayers.get());
  }
}
