package com.faforever.client.achievements;

import com.faforever.client.config.CacheNames;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.lobby.UpdatedAchievementsInfo;
import com.faforever.commons.lobby.UpdatedAchievementsInfo.AchievementState;
import com.google.common.annotations.VisibleForTesting;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.github.nocatch.NoCatch.noCatch;


@Lazy
@Service
@RequiredArgsConstructor
// TODO cut dependencies if possible
public class AchievementService implements InitializingBean {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;

  private final FafService fafService;
  private final PlayerService playerService;
  private final AssetService assetService;
  @VisibleForTesting
  final ObservableList<PlayerAchievement> playerAchievements = FXCollections.observableArrayList();
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(Integer playerId) {
    int currentPlayerId = playerService.getCurrentPlayer().getId();
    if (Objects.equals(currentPlayerId, playerId)) {
      if (readOnlyPlayerAchievements.isEmpty()) {

        return reloadAchievements();
      }
      return CompletableFuture.completedFuture(readOnlyPlayerAchievements);
    }

    return fafService.getPlayerAchievements(playerId);
  }


  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return fafService.getAchievementDefinitions();
  }


  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return fafService.getAchievementDefinition(achievementId);
  }


  @Cacheable(value = CacheNames.ACHIEVEMENT_IMAGES, sync = true)
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

  private CompletableFuture<List<PlayerAchievement>> reloadAchievements() {
    CompletableFuture<List<PlayerAchievement>> achievementsLoadedFuture = new CompletableFuture<>();
    int playerId = playerService.getCurrentPlayer().getId();
    fafService.getPlayerAchievements(playerId).thenAccept(achievements -> {
      playerAchievements.setAll(achievements);
      achievementsLoadedFuture.complete(readOnlyPlayerAchievements);
    });
    return achievementsLoadedFuture;
  }

  @Override
  public void afterPropertiesSet() {
    fafService.addOnMessageListener(UpdatedAchievementsInfo.class, updatedAchievementsMessage -> reloadAchievements());
  }

}
