package com.faforever.client.mod;


import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewsSummary;
import com.faforever.client.vault.review.ReviewsSummaryBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class ModVersionBuilder {
  private final ModVersion modVersion = new ModVersion();

  public static ModVersionBuilder create() {
    return new ModVersionBuilder();
  }

  public ModVersionBuilder defaultValues() throws MalformedURLException {
    displayName("Test Mod");
    imagePath(Paths.get("."));
    id("test");
    uid("uid");
    description("This is a test mod");
    uploader("junit");
    selectable(true);
    version(new ComparableVersion("1"));
    thumbnailUrl(new URL("https://www.google.com"));
    comments(FXCollections.observableArrayList());
    selected(false);
    likes(0);
    played(100);
    createTime(LocalDateTime.MIN);
    updateTime(LocalDateTime.MAX);
    downloadUrl(new URL("https://www.google.com"));
    hookDirectories(FXCollections.observableArrayList());
    reviewsSummary(ReviewsSummaryBuilder.create().defaultValues().get());
    modType(ModType.UI);
    filename("foo.tmp");
    icon("testIcon");
    ranked(true);
    hidden(false);
    return this;
  }


  public ModVersionBuilder displayName(String displayName) {
    modVersion.setDisplayName(displayName);
    return this;
  }

  public ModVersionBuilder imagePath(Path imagePath) {
    modVersion.setImagePath(imagePath);
    return this;
  }

  public ModVersionBuilder id(String id) {
    modVersion.setId(id);
    return this;
  }

  public ModVersionBuilder uid(String uid) {
    modVersion.setUid(uid);
    return this;
  }

  public ModVersionBuilder description(String description) {
    modVersion.setDescription(description);
    return this;
  }

  public ModVersionBuilder uploader(String uploader) {
    modVersion.setUploader(uploader);
    return this;
  }

  public ModVersionBuilder selectable(boolean selectable) {
    modVersion.setSelectable(selectable);
    return this;
  }

  public ModVersionBuilder version(ComparableVersion version) {
    modVersion.setVersion(version);
    return this;
  }

  public ModVersionBuilder thumbnailUrl(URL thumbnailUrl) {
    modVersion.setThumbnailUrl(thumbnailUrl);
    return this;
  }

  public ModVersionBuilder comments(ObservableList<String> comments) {
    modVersion.setComments(comments);
    return this;
  }

  public ModVersionBuilder selected(boolean selected) {
    modVersion.setSelected(selected);
    return this;
  }

  public ModVersionBuilder likes(int likes) {
    modVersion.setLikes(likes);
    return this;
  }

  public ModVersionBuilder played(int played) {
    modVersion.setPlayed(played);
    return this;
  }

  public ModVersionBuilder createTime(LocalDateTime createTime) {
    modVersion.setCreateTime(createTime);
    return this;
  }

  public ModVersionBuilder updateTime(LocalDateTime updateTime) {
    modVersion.setUpdateTime(updateTime);
    return this;
  }

  public ModVersionBuilder downloadUrl(URL downloadUrl) {
    modVersion.setDownloadUrl(downloadUrl);
    return this;
  }

  public ModVersionBuilder hookDirectories(ObservableList<String> hookDirectories) {
    modVersion.getHookDirectories().setAll(hookDirectories);
    return this;
  }

  public ModVersionBuilder reviews(ObservableList<Review> reviews) {
    modVersion.setReviews(reviews);
    return this;
  }

  public ModVersionBuilder reviewsSummary(ReviewsSummary reviewsSummary) {
    modVersion.setReviewsSummary(reviewsSummary);
    return this;
  }

  public ModVersionBuilder modType(ModType modType) {
    modVersion.setModType(modType);
    return this;
  }

  public ModVersionBuilder filename(String filename) {
    modVersion.setFilename(filename);
    return this;
  }

  public ModVersionBuilder icon(String icon) {
    modVersion.setIcon(icon);
    return this;
  }

  public ModVersionBuilder ranked(boolean ranked) {
    modVersion.setRanked(ranked);
    return this;
  }

  public ModVersionBuilder hidden(boolean hidden) {
    modVersion.setHidden(hidden);
    return this;
  }

  public ModVersionBuilder mod(Mod mod) {
    modVersion.setMod(mod);
    return this;
  }

  public ModVersion get() {
    return modVersion;
  }
}
