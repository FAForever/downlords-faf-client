package com.faforever.client.mod;

import com.faforever.client.vault.review.ReviewsSummary;
import com.faforever.client.vault.review.ReviewsSummaryBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;

public class ModBuilder {
  private final Mod mod = new Mod();

  public static ModBuilder create() {
    return new ModBuilder();
  }

  public ModBuilder defaultValues() throws MalformedURLException {
    id("test");
    displayName("Test Mod");
    author("junit");
    createTime(OffsetDateTime.MIN);
    updateTime(OffsetDateTime.MAX);
    uploader("junit");
    reviewsSummary(ReviewsSummaryBuilder.create().defaultValues().get());
    ModVersion modVersion = ModVersionBuilder.create().defaultValues().get();
    latestVersion(modVersion);
    versions(FXCollections.observableArrayList(modVersion));
    return this;
  }


  public ModBuilder id(String id) {
    mod.setId(id);
    return this;
  }

  public ModBuilder displayName(String displayName) {
    mod.setDisplayName(displayName);
    return this;
  }

  public ModBuilder author(String author) {
    mod.setAuthor(author);
    return this;
  }

  public ModBuilder createTime(OffsetDateTime createTime) {
    mod.setCreateTime(createTime);
    return this;
  }

  public ModBuilder updateTime(OffsetDateTime updateTime) {
    mod.setUpdateTime(updateTime);
    return this;
  }

  public ModBuilder uploader(String uploader) {
    mod.setUploader(uploader);
    return this;
  }

  public ModBuilder reviewsSummary(ReviewsSummary reviewsSummary) {
    mod.setReviewsSummary(reviewsSummary);
    return this;
  }

  public ModBuilder versions(ObservableList<ModVersion> versions) {
    mod.getVersions().setAll(versions);
    return this;
  }

  public ModBuilder latestVersion(ModVersion latestVersion) {
    mod.setLatestVersion(latestVersion);
    return this;
  }

  public Mod get() {
    return mod;
  }
}
