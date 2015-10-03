package com.faforever.client.play;

import javafx.beans.property.BooleanProperty;

import java.io.IOException;

public interface PlayServices {

  void authorize(String uid);

  void incrementPlayedCustomGames() throws IOException;

  void incrementPlayedRanked1v1Games() throws IOException;

  BooleanProperty authorizedProperty();
}
