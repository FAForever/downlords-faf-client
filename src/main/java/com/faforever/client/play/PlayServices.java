package com.faforever.client.play;

import com.google.api.client.auth.oauth2.Credential;

import java.io.IOException;
import java.util.concurrent.Future;

public interface PlayServices {

  Future<Credential> authorize(String uid);

  void incrementPlayedCustomGames() throws IOException;

  void incrementPlayedRanked1v1Games() throws IOException;

}
