package com.faforever.client.games;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.message.GameLaunchMessage;
import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.supcom.SupComService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameServiceImpl implements GameService {

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  UserService userService;

  @Autowired
  SupComService supComService;

  @Override
  public void publishPotentialPlayer() {
    String username = userService.getUsername();
//    serverAccessor.publishPotentialPlayer(username);
  }

  @Override
  public void addOnGameInfoListener(OnGameInfoMessageListener listener) {
    serverAccessor.addOnGameInfoMessageListener(listener);
  }

  @Override
  public void createGame(NewGameInfo newGameInfo, Callback<Void> callback) {
    serverAccessor.requestNewGame(newGameInfo, new Callback<GameLaunchMessage>() {
      @Override
      public void success(GameLaunchMessage gameLaunchMessage) {
        List<String> args = fixMalformedArgs(gameLaunchMessage.getArgs());

        supComService.startGame(gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), args, callback);
      }

      @Override
      public void error(Throwable e) {

      }
    });
  }

  private List<String> fixMalformedArgs(List<String> gameLaunchMessage) {
    ArrayList<String> fixedArgs = new ArrayList<>();

    for (String combinedArg : gameLaunchMessage) {
      String[] split = combinedArg.split(" ");

      Collections.addAll(fixedArgs, split);
    }
    return fixedArgs;
  }
}
