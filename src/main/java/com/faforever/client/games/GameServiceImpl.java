package com.faforever.client.games;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.message.GameLaunchMessage;
import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.supcom.SupComService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        try {
          Process process = supComService.startGame(gameLaunchMessage.getUid(), gameLaunchMessage.getMod(), args);
          serverAccessor.notifyGameStarted();
          waitForProcessTerminationInBackground(process);
          callback.success(null);
        } catch (Exception e) {
          callback.error(e);
        }
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Could not create game", e);
      }
    });
  }

  private void waitForProcessTerminationInBackground(Process process) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int exitCode = process.waitFor();
        logger.info("SupCom terminated with exit code {}", exitCode);
        return null;
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
