package com.faforever.client.play;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.base.MoreObjects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class PlayServicesImpl implements PlayServices {

  static final String ACH_NOVICE = "c6e6039f-c543-424e-ab5f-b34df1336e81";
  static final String ACH_JUNIOR = "d5c759fe-a1a8-4103-888d-3ba319562867";
  static final String ACH_SENIOR = "6a37e2fc-1609-465e-9eca-91eeda4e63c4";
  static final String ACH_VETERAN = "bd12277a-6604-466a-9ee6-af6908573585";
  static final String ACH_ADDICT = "805f268c-88aa-4073-aa2b-ea30700f70d6";
  static final String ACH_FIRST_SUCCESS = "5b7ec244-58c0-40ca-9d68-746b784f0cad";
  static final String ACH_PRIVATE = "203844f6-b12a-4ca3-9215-fa1d1ae00ff4";
  static final String ACH_CORPORAL = "ea17c16e-d1b8-4b64-bd3d-3e09e7fc6b4d";
  static final String ACH_SERGEANT_MAJOR = "e31d04d1-348c-423e-a3aa-d46711538b7b";
  static final String ACH_GETTING_STARTED = "01487e60-6d44-409b-83d8-f5f77272c364";
  static final String ACH_GETTING_BETTER = "181ea296-207e-4da0-9ebb-2db66c2c7e0c";
  static final String ACH_GETTING_PRO = "5e215a3e-cf54-44f4-9f5a-50cd94258efc";
  static final String ACH_HATTRICK = "08629902-8e18-4d92-ad14-c8ecde4a8674";
  static final String ACH_THAT_WAS_CLOSE = "290df67c-eb01-4fe7-9e32-caae1c10442f";
  static final String ACH_TOP_SCORE = "305a8d34-42fd-42f3-ba91-d9f5e437a9a6";
  static final String ACH_UNBEATABLE = "d3d2c78b-d42d-4b65-99b8-a350f119f898";
  static final String ACH_RUSHER = "02081bb0-3b7a-4a36-99ef-5ae5d92d7146";
  static final String ACH_MA12_STRIKER = "1a3ad9e0-53eb-47d0-9404-14dbcefbed9b";
  static final String ACH_RIPTIDE = "326493d7-ce2c-4a43-bbc8-3e990e2685a1";
  static final String ACH_DEMOLISHER = "7d6d8c55-3e2a-41d0-a97e-d35513af1ec6";
  static final String ACH_MANTIS = "d1d50fbb-7fe9-41b0-b667-4433704b8a2c";
  static final String ACH_WAGNER = "af161922-3e52-4600-9161-d850ab0fae86";
  static final String ACH_TREBUCHET = "ff23024e-f533-4e23-8f8f-ecc21d5283f8";
  static final String ACH_AURORA = "d656ade4-e054-415a-a2e9-5f4105f7d724";
  static final String ACH_BLAZE = "06a39447-66a3-4160-93d5-d48337b0cbb5";
  static final String ACH_SERENITY = "7f993f98-dbec-41a5-9c9a-5f85edf30767";
  static final String ACH_THAAM = "c964ac69-b146-43d0-bd7a-cd22144f9983";
  static final String ACH_YENZYNE = "7aa7fc88-48a2-4e49-9cd7-35e2f6ce4cec";
  static final String ACH_SUTHANUS = "6acc8bc6-1fd3-4c33-97a1-85dfed6d167a";
  static final String ACH_LANDLUBBER = "53173f4d-450c-46f0-ac59-85834cc74972";
  static final String ACH_SEAMAN = "2d5cd544-4fc8-47b9-8ebb-e72ed6423d51";
  static final String ACH_ADMIRAL_OF_THE_FLEET = "bd77964b-c06b-4649-bf7c-d35cb7715854";
  static final String ACH_WRIGHT_BROTHER = "c1ccde26-8449-4625-b769-7d8f75fa8df3";
  static final String ACH_WINGMAN = "a4ade3d4-d541-473f-9788-e92339446d75";
  static final String ACH_KING_OF_THE_SKIES = "e220d5e6-481c-4347-ac69-b6b6f956bc0f";
  static final String ACH_MILITIAMAN = "e5c63aec-20a0-4263-841d-b7bc45209713";
  static final String ACH_GRENADIER = "ec8faec7-e3e1-436e-a1ac-9f7adc3d0387";
  static final String ACH_FIELD_MARSHAL = "10f17c75-1154-447d-a4f7-6217add0407e";
  static final String ACH_TECHIE = "06b19364-5aab-4bce-883d-975f663d2091";
  static final String ACH_I_LOVE_BIG_TOYS = "cd64c5e7-b063-4543-9f52-0e87883b33a9";
  static final String ACH_EXPERIMENTALIST = "e8af7cc9-aaa6-4d0e-8e5a-481702a83a4e";
  static final String ACH_WHAT_A_SWARM = "045342e1-ae0d-4ef6-98bc-0bb54ffe00b3";
  static final String ACH_THE_TRANSPORTER = "d38aec23-e487-4aa2-899e-418e29ffbd36";
  static final String ACH_WHO_NEEDS_SUPPORT = "eb1ee9ab-4828-417b-b3e8-c8281ee7a353";
  static final String ACH_DEADLY_BUGS = "e7645e7c-7456-48a8-a562-d97521498e7e";
  static final String ACH_NO_MERCY = "f0cde5d8-4933-4074-a2fb-819074d21abd";
  static final String ACH_FLYING_DEATH = "a98fcfaf-29ac-4526-84c2-44f284518f8c";
  static final String ACH_INCOMING_ROBOTS = "1c8fcb6f-a5b6-497f-8b0d-ac5ac6fef408";
  static final String ACH_ARACHNOLOGIST = "a1f87fb7-67ca-4a86-afc6-f23a41b40e9f";
  static final String ACH_HOLY_CRAB = "db141e87-5818-435f-80a3-08cc6f1fdac6";
  static final String ACH_FATTER_IS_BETTER = "ab241de5-e773-412e-b073-090da8e38c4c";
  static final String ACH_ALIEN_INVASION = "1f140add-b0ae-4e02-91a0-45d62b988a22";
  static final String ACH_ASS_WASHER = "60d1e60d-036b-491e-a992-2b18321848c2";
  static final String ACH_DEATH_FROM_ABOVE = "539da20b-5026-4c49-8e22-e4a305d58845";
  static final String ACH_STORMY_SEA = "e603f306-ba6b-4507-9556-37a308e5a722";
  static final String ACH_IT_AINT_A_CITY = "a909629f-46f5-469e-afd1-192d42f55e4d";
  static final String ACH_RAINMAKER = "50260d04-90ff-45c8-816b-4ad8d7b97ecd";
  static final String ACH_I_HAVE_A_CANON = "31a728f8-ace9-45fa-a3f2-57084bc9e461";
  static final String ACH_MAKE_IT_HAIL = "987ca192-26e1-4b96-b593-40c115451cc0";
  static final String ACH_SO_MUCH_RESOURCES = "46a6e900-88bb-4eae-92d1-4f31b53faedc";
  static final String ACH_NUCLEAR_WAR = "9ad697bb-441e-45a5-b682-b9227e8eef3e";
  static final String ACH_DR_EVIL = "a6b7dfa1-1ebc-4c6d-9305-4a9d623e1b4f";
  static final String ACH_DONT_MESS_WITH_ME = "2103e0de-1c87-4fba-bc1b-0bba66669607";
  static final String EVENT_CUSTOM_GAMES_PLAYED = "cfa449a6-655b-48d5-9a27-6044804fe35c";
  static final String EVENT_RANKED_1V1_GAMES_PLAYED = "4a929def-e347-45b4-b26d-4325a3115859";
  static final String EVENT_FALLEN_ACUS = "d6a699b7-99bc-4a7f-b128-15e1e289a7b3";
  static final String EVENT_BUILT_AIR_UNITS = "3ebb0c4d-5e92-4446-bf52-d17ba9c5cd3c";
  static final String EVENT_FALLEN_AIR_UNITS = "225e9b2e-ae09-4ae1-a198-eca8780b0fcd";
  static final String EVENT_BUILT_LAND_UNITS = "ea123d7f-bb2e-4a71-bd31-88859f0c3c00";
  static final String EVENT_FALLEN_LAND_UNITS = "a1a3fd33-abe2-4e56-800a-b72f4c925825";
  static final String EVENT_BUILT_NAVAL_UNITS = "b5265b42-1747-4ba1-936c-292202637ce6";
  static final String EVENT_FALLEN_NAVAL_UNITS = "3a7b3667-0f79-4ac7-be63-ba841fd5ef05";
  static final String EVENT_SECONDS_PLAYED = "cc791f00-343c-48d4-b5b3-8900b83209c0";
  static final String EVENT_BUILT_TECH_1_UNITS = "a8ee4f40-1e30-447b-bc2c-b03065219795";
  static final String EVENT_FALLEN_TECH_1_UNITS = "3dd3ed78-ce78-4006-81fd-10926738fbf3";
  static final String EVENT_BUILT_TECH_2_UNITS = "89d4f391-ed2d-4beb-a1ca-6b93db623c04";
  static final String EVENT_FALLEN_TECH_2_UNITS = "aebd750b-770b-4869-8e37-4d4cfdc480d0";
  static final String EVENT_BUILT_TECH_3_UNITS = "92617974-8c1f-494d-ab86-65c2a95d1486";
  static final String EVENT_FALLEN_TECH_3_UNITS = "7f15c2be-80b7-4573-8f41-135f84773e0f";
  static final String EVENT_BUILT_EXPERIMENTALS = "ed9fd79d-5ec7-4243-9ccf-f18c4f5baef1";
  static final String EVENT_FALLEN_EXPERIMENTALS = "701ca426-0943-4931-85af-6a08d36d9aaa";
  static final String EVENT_BUILT_ENGINEERS = "60bb1fc0-601b-45cd-bd26-83b1a1ac979b";
  static final String EVENT_FALLEN_ENGINEERS = "e8e99a68-de1b-4676-860d-056ad2207119";
  static final String EVENT_AEON_PLAYS = "96ccc66a-c5a0-4f48-acaa-888b00778b57";
  static final String EVENT_AEON_WINS = "a6b51c26-64e6-4e7a-bda7-ea1cfe771ebb";
  static final String EVENT_CYBRAN_PLAYS = "ad193982-e7ca-465c-80b0-5493f9739559";
  static final String EVENT_CYBRAN_WINS = "56b06197-1890-42d0-8b59-25e1add8dc9a";
  static final String EVENT_UEF_PLAYS = "1b900d26-90d2-43d0-a64e-ed90b74c3704";
  static final String EVENT_UEF_WINS = "7be6fdc5-7867-4467-98ce-f7244a66625a";
  static final String EVENT_SERAPHIM_PLAYS = "fefcb392-848f-4836-9683-300b283bc308";
  static final String EVENT_SERAPHIM_WINS = "15b6c19a-6084-4e82-ada9-6c30e282191f";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ReentrantLock BATCH_REQUEST_LOCK = new ReentrantLock();
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Resource
  PreferencesService preferencesService;
  @Resource
  UserService userService;
  @Resource
  CloudAccessor cloudAccessor;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  FafApiAccessor fafApiAccessor;

  private AchievementUpdatesRequest achievementUpdatesRequest;
  private EventUpdatesRequest eventUpdatesRequest;

  public PlayServicesImpl() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  @Async
  public CompletableFuture<Void> authorize() {
    fafApiAccessor.authorize(userService.getUid());
    loadCurrentPlayerAchievements();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void startBatchUpdate() {
    BATCH_REQUEST_LOCK.lock();
//    try {
    // FIXME implement as soon as OAuth is available
//      authorize().get();
    logger.debug("Starting batch update");
    achievementUpdatesRequest = new AchievementUpdatesRequest();
    eventUpdatesRequest = new EventUpdatesRequest();
//    } catch (InterruptedException | ExecutionException e) {
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public void executeBatchUpdate() {
    int updatedAchievements;
    int updatedEvents;

    try {
      checkBatchRequest();
      updatedAchievements = executeAchievementUpdates();
      updatedEvents = executeEventUpdates();
    } finally {
      resetBatchUpdate();
    }

    if (updatedAchievements != 0 || updatedEvents != 0) {
      loadCurrentPlayerAchievements();
    }
  }

  @Override
  public void customGamePlayed() {
    recordEvent(EVENT_CUSTOM_GAMES_PLAYED, 1L);
  }

  @Override
  public void ranked1v1GamePlayed() {
    recordEvent(EVENT_RANKED_1V1_GAMES_PLAYED, 1L);
  }

  @Override
  public void ranked1v1GameWon() {
    unlockAchievement(ACH_FIRST_SUCCESS);
  }

  @Override
  public void killedCommanders(int count, boolean survived) {
    if (count >= 3 && survived) {
      unlockAchievement(ACH_HATTRICK);
    }
    if (count > 0) {
      incrementAchievement(ACH_DONT_MESS_WITH_ME, count);
    }
    if (!survived) {
      recordEvent(EVENT_FALLEN_ACUS, 1L);
    }
  }

  @Override
  public void acuDamageReceived(double damage, boolean survived) {
    if (damage < 12_000 || !survived) {
      return;
    }
    unlockAchievement(ACH_THAT_WAS_CLOSE);
  }

  @Override
  public void resetBatchUpdate() {
    achievementUpdatesRequest = null;
    if (BATCH_REQUEST_LOCK.isLocked()) {
      BATCH_REQUEST_LOCK.unlock();
    }
  }

  @Override
  public void topScoringPlayer(int totalPlayers) {
    if (totalPlayers < 8) {
      return;
    }
    unlockAchievement(ACH_TOP_SCORE);
    incrementAchievement(ACH_UNBEATABLE, 1);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    if (userService.getUsername().equals(username)) {
      return readOnlyPlayerAchievements;
    }

    ObservableList<PlayerAchievement> playerAchievements = FXCollections.observableArrayList();

    cloudAccessor.getPlayerIdForUsername(username)
        .<List<PlayerAchievement>>thenApply(playerId -> {
          if (StringUtils.isEmpty(playerId)) {
            return null;
          }
          return fafApiAccessor.getPlayerAchievements(Integer.parseInt(playerId));
        })
        .thenAccept(playerAchievements::setAll)
        .exceptionally(throwable -> {
          logger.warn("Could not load achievements for player: " + username, throwable);
          return null;
        });

    return playerAchievements;
  }

  @Override
  @Async
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinitions());
  }

  @Override
  public void playerRating1v1(int rating) {
    setStepsAtLeast(ACH_PRIVATE, rating);
    setStepsAtLeast(ACH_CORPORAL, rating);
    setStepsAtLeast(ACH_SERGEANT_MAJOR, rating);
  }

  @Override
  public void playerRatingGlobal(int rating) {
    setStepsAtLeast(ACH_GETTING_STARTED, rating);
    setStepsAtLeast(ACH_GETTING_BETTER, rating);
    setStepsAtLeast(ACH_GETTING_PRO, rating);
  }

  @Override
  public void wonWithinDuration(Duration duration) {
    if (duration.compareTo(Duration.ofMinutes(15)) >= 0) {
      return;
    }
    unlockAchievement(ACH_RUSHER);
  }

  @Override
  public void factionPlayed(Faction faction, boolean survived) {
    switch (faction) {
      case AEON:
        recordEvent(EVENT_AEON_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_AEON_WINS, 1);
          incrementAchievement(ACH_AURORA, 1);
          incrementAchievement(ACH_BLAZE, 1);
          incrementAchievement(ACH_SERENITY, 1);
        }
        break;
      case CYBRAN:
        recordEvent(EVENT_CYBRAN_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_CYBRAN_WINS, 1);
          incrementAchievement(ACH_MANTIS, 1);
          incrementAchievement(ACH_WAGNER, 1);
          incrementAchievement(ACH_TREBUCHET, 1);
        }
        break;
      case UEF:
        recordEvent(EVENT_UEF_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_UEF_WINS, 1);
          incrementAchievement(ACH_MA12_STRIKER, 1);
          incrementAchievement(ACH_RIPTIDE, 1);
          incrementAchievement(ACH_DEMOLISHER, 1);
        }
        break;
      case SERAPHIM:
        recordEvent(EVENT_SERAPHIM_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_SERAPHIM_WINS, 1);
          incrementAchievement(ACH_THAAM, 1);
          incrementAchievement(ACH_YENZYNE, 1);
          incrementAchievement(ACH_SUTHANUS, 1);
        }
        break;
    }
  }

  @Override
  public void unitStats(int airBuilt, int airKilled, int landBuilt, int landKilled, int navalBuilt, int navalKilled,
                        int tech1Built, int tech1Killed, int tech2Built, int tech2Killed, int tech3Built,
                        int tech3Killed, int experimentalsBuilt, int experimentalsKilled, int engineersBuilt,
                        int engineersKilled, boolean survived) {
    recordEvent(EVENT_BUILT_AIR_UNITS, airBuilt);
    recordEvent(EVENT_BUILT_LAND_UNITS, landBuilt);
    recordEvent(EVENT_BUILT_NAVAL_UNITS, navalBuilt);
    recordEvent(EVENT_BUILT_TECH_1_UNITS, tech1Built);
    recordEvent(EVENT_BUILT_TECH_2_UNITS, tech2Built);
    recordEvent(EVENT_BUILT_TECH_3_UNITS, tech3Built);
    recordEvent(EVENT_BUILT_EXPERIMENTALS, experimentalsBuilt);
    recordEvent(EVENT_BUILT_ENGINEERS, engineersBuilt);

    recordEvent(EVENT_FALLEN_AIR_UNITS, airKilled);
    recordEvent(EVENT_FALLEN_LAND_UNITS, landKilled);
    recordEvent(EVENT_FALLEN_NAVAL_UNITS, navalKilled);
    recordEvent(EVENT_FALLEN_TECH_1_UNITS, tech1Killed);
    recordEvent(EVENT_FALLEN_TECH_2_UNITS, tech2Killed);
    recordEvent(EVENT_FALLEN_TECH_3_UNITS, tech3Killed);
    recordEvent(EVENT_FALLEN_EXPERIMENTALS, experimentalsKilled);
    recordEvent(EVENT_FALLEN_ENGINEERS, engineersKilled);

    if (survived) {
      if (airBuilt > landBuilt && airBuilt > navalBuilt) {
        incrementAchievement(ACH_WRIGHT_BROTHER, 1);
        incrementAchievement(ACH_WINGMAN, 1);
        incrementAchievement(ACH_KING_OF_THE_SKIES, 1);
      } else if (landBuilt > airBuilt && landBuilt > navalBuilt) {
        incrementAchievement(ACH_MILITIAMAN, 1);
        incrementAchievement(ACH_GRENADIER, 1);
        incrementAchievement(ACH_FIELD_MARSHAL, 1);
      } else if (navalBuilt > airBuilt && navalBuilt > landBuilt) {
        incrementAchievement(ACH_LANDLUBBER, 1);
        incrementAchievement(ACH_SEAMAN, 1);
        incrementAchievement(ACH_ADMIRAL_OF_THE_FLEET, 1);
      }
      if (experimentalsBuilt >= 3) {
        incrementAchievement(ACH_TECHIE, 1);
        incrementAchievement(ACH_I_LOVE_BIG_TOYS, 1);
        incrementAchievement(ACH_EXPERIMENTALIST, 1);
      }
      if (experimentalsBuilt > 0) {
        incrementAchievement(ACH_DR_EVIL, experimentalsBuilt);
      }
    }
  }

  @Override
  public void timePlayed(Duration duration, boolean survived) {
    recordEvent(EVENT_SECONDS_PLAYED, duration.getSeconds());
  }

  @Override
  public void asfBuilt(int count) {
    if (count >= 150) {
      unlockAchievement(ACH_WHAT_A_SWARM);
    }
  }

  @Override
  public void builtTransports(int count) {
    if (count > 0) {
      incrementAchievement(ACH_THE_TRANSPORTER, count);
    }
  }

  @Override
  public void builtParagons(int count, boolean survived) {
    if (survived && count > 0) {
      unlockAchievement(ACH_SO_MUCH_RESOURCES);
    }
  }

  @Override
  public void builtYolonaOss(int count, boolean survived) {
    if (survived && count > 0) {
      unlockAchievement(ACH_NUCLEAR_WAR);
    }
  }

  @Override
  public void builtScathis(int count, boolean survived) {
    if (survived && count > 0) {
      unlockAchievement(ACH_MAKE_IT_HAIL);
    }
  }

  @Override
  public void builtSalvations(int count, boolean survived) {
    if (survived && count > 0) {
      unlockAchievement(ACH_RAINMAKER);
    }
  }

  @Override
  public void builtMavors(int count, boolean survived) {
    if (survived && count > 0) {
      unlockAchievement(ACH_I_HAVE_A_CANON);
    }
  }

  @Override
  public void builtAtlantis(int count) {
    if (count > 0) {
      incrementAchievement(ACH_IT_AINT_A_CITY, count);
    }
  }

  @Override
  public void builtTempests(int count) {
    if (count > 0) {
      incrementAchievement(ACH_STORMY_SEA, count);
    }
  }

  @Override
  public void builtCzars(int count) {
    if (count > 0) {
      incrementAchievement(ACH_DEATH_FROM_ABOVE, count);
    }
  }

  @Override
  public void builtAhwasshas(int count) {
    if (count > 0) {
      incrementAchievement(ACH_ASS_WASHER, count);
    }
  }

  @Override
  public void builtYthothas(int count) {
    if (count > 0) {
      incrementAchievement(ACH_ALIEN_INVASION, count);
    }
  }

  @Override
  public void builtFatboys(int count) {
    if (count > 0) {
      incrementAchievement(ACH_FATTER_IS_BETTER, count);
    }
  }

  @Override
  public void builtMonkeylords(int count) {
    if (count > 0) {
      incrementAchievement(ACH_ARACHNOLOGIST, count);
    }
  }

  @Override
  public void builtGalacticColossus(int count) {
    if (count > 0) {
      incrementAchievement(ACH_INCOMING_ROBOTS, count);
    }
  }

  @Override
  public void builtSoulRippers(int count) {
    if (count > 0) {
      incrementAchievement(ACH_FLYING_DEATH, count);
    }
  }

  @Override
  public void builtMercies(int count) {
    if (count > 0) {
      incrementAchievement(ACH_NO_MERCY, count);
    }
  }

  @Override
  public void builtFireBeetles(int count) {
    if (count > 0) {
      incrementAchievement(ACH_DEADLY_BUGS, count);
    }
  }

  @Override
  public void builtSupportCommanders(int count) {
    if (count >= 10) {
      unlockAchievement(ACH_WHO_NEEDS_SUPPORT);
    }
  }

  @Override
  public void builtMegaliths(int count) {
    if (count > 0) {
      incrementAchievement(ACH_HOLY_CRAB, count);
    }
  }

  @Override
  public void numberOfGamesPlayed(int numberOfGames) {
    setStepsAtLeast(ACH_NOVICE, numberOfGames);
    setStepsAtLeast(ACH_JUNIOR, numberOfGames);
    setStepsAtLeast(ACH_SENIOR, numberOfGames);
    setStepsAtLeast(ACH_VETERAN, numberOfGames);
    setStepsAtLeast(ACH_ADDICT, numberOfGames);
  }

  private void setStepsAtLeast(String achievementId, int steps) {
    checkBatchRequest();
    achievementUpdatesRequest.setStepsAtLeast(achievementId, steps);
  }

  private void incrementAchievement(String achievementId, int steps) {
    checkBatchRequest();
    achievementUpdatesRequest.increment(achievementId, steps);
  }

  private void unlockAchievement(String achievementId) {
    checkBatchRequest();
    achievementUpdatesRequest.unlock(achievementId);
  }

  private void recordEvent(String eventId, long updateCount) {
    checkBatchRequest();
    eventUpdatesRequest.record(eventId, updateCount);
  }

  private void checkBatchRequest() {
    if (achievementUpdatesRequest == null) {
      throw new IllegalStateException("Batch update has not been started");
    }
  }

  private int executeAchievementUpdates() {
    if (achievementUpdatesRequest.getUpdates().isEmpty()) {
      return 0;
    }

    List<UpdatedAchievement> result = fafApiAccessor.executeAchievementUpdates(achievementUpdatesRequest, userService.getUid());
    notifyAboutUnlockedAchievements(result);
    return result.size();
  }

  private int executeEventUpdates() {
    if (eventUpdatesRequest.getUpdates().isEmpty()) {
      return 0;
    }
    fafApiAccessor.recordEvents(eventUpdatesRequest, userService.getUid());
    return 0;
  }

  private void notifyAboutUnlockedAchievements(List<UpdatedAchievement> updatedAchievements) {
    updatedAchievements.stream()
        .filter(UpdatedAchievement::getNewlyUnlocked)
        .forEach(updatedAchievement -> {
          AchievementDefinition achievementDefinition = fafApiAccessor.getAchievementDefinition(updatedAchievement.getAchievementId());

          if (updatedAchievement.getNewlyUnlocked()) {
            // TODO use proper image
            String imageUrl = MoreObjects.firstNonNull(
                achievementDefinition.getUnlockedIconUrl(),
                getClass().getResource("/images/tray_icon.png").toString()
            );

            notificationService.addNotification(new TransientNotification(
                i18n.get("achievement.unlockedTitle"),
                achievementDefinition.getName(),
                new Image(imageUrl)
            ));
          }
        });
  }

  private void loadCurrentPlayerAchievements() {
    JavaFxUtil.assertBackgroundThread();
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUid()));
  }
}

