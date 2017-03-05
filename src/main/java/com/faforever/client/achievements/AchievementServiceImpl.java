package com.faforever.client.achievements;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.config.CacheNames;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.github.nocatch.NoCatch.noCatch;


@Lazy
@Service
public class AchievementServiceImpl implements AchievementService {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  private final UserService userService;
  private final FafService fafService;
  private final PlayerService playerService;
  private final AssetService assetService;

  @Inject
  // TODO cut dependencies if possible
  public AchievementServiceImpl(UserService userService, FafService fafService,
                                NotificationService notificationService, I18n i18n, PlayerService playerService,
                                UiService uiService, AssetService assetService) {
    this.userService = userService;
    this.fafService = fafService;
    this.playerService = playerService;
    this.assetService = assetService;

    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(String username) {
    if (userService.getUsername().equalsIgnoreCase(username)) {
      if (readOnlyPlayerAchievements.isEmpty()) {
        reloadAchievements();
      }
      return CompletableFuture.completedFuture(readOnlyPlayerAchievements);
    }

    Player playerForUsername = playerService.getPlayerForUsername(username);
    if (playerForUsername == null) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int playerId = playerForUsername.getId();

    return fafService.getPlayerAchievements(playerId);
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return fafService.getAchievementDefinitions();
  }

  @Override
  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return fafService.getAchievementDefinition(achievementId);
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENT_IMAGES)
  public Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState) {
    URL url;
    switch (achievementState) {
      case REVEALED:
        url = noCatch(() -> new URL(achievementDefinition.getRevealedIconUrl()));
        break;
      case UNLOCKED:
        url = noCatch(() -> new URL(achievementDefinition.getUnlockedIconUrl()));
        break;
      default:
        throw new UnsupportedOperationException("Not yet implemented");
    }
    return assetService.loadAndCacheImage(url, Paths.get("achievements").resolve(achievementState.name().toLowerCase()),
        null, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE);
  }

  private void reloadAchievements() {
    fafService.getPlayerAchievements(userService.getUserId()).thenAccept(playerAchievements::setAll);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(UpdatedAchievementsMessage.class, updatedAchievementsMessage -> reloadAchievements());
  }
}
