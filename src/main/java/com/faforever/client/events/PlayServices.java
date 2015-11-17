package com.faforever.client.events;

import javafx.collections.ObservableList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PlayServices {

  CompletableFuture<Void> authorize();

  ObservableList<PlayerAchievement> getPlayerAchievements(String username);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

}
