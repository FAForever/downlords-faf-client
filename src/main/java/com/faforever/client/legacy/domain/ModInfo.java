package com.faforever.client.legacy.domain;

import java.net.URL;
import java.util.List;

public class ModInfo {

  private String thumbnail;
  private URL link;
  private List<String> bugreports;
  private List<String> comments;
  private String description;
  private int played;
  private int likes;
  private int downloads;
  private double date;
  private String uid;
  private String name;
  private String version;
  private String author;
  private int ui;
  private int big;
  private int small;

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public URL getLink() {
    return link;
  }

  public void setLink(URL link) {
    this.link = link;
  }

  public List<String> getBugreports() {
    return bugreports;
  }

  public void setBugreports(List<String> bugreports) {
    this.bugreports = bugreports;
  }

  public List<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getPlayed() {
    return played;
  }

  public void setPlayed(int played) {
    this.played = played;
  }

  public int getDownloads() {
    return downloads;
  }

  public void setDownloads(int downloads) {
    this.downloads = downloads;
  }

  public double getDate() {
    return date;
  }

  public void setDate(double date) {
    this.date = date;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public int isUi() {
    return ui;
  }

  public void setUi(int ui) {
    this.ui = ui;
  }

  public int isBig() {
    return big;
  }

  public void setBig(int big) {
    this.big = big;
  }

  public int isSmall() {
    return small;
  }

  public void setSmall(int small) {
    this.small = small;
  }

  public int getLikes() {
    return likes;
  }

  public void setLikes(int likes) {
    this.likes = likes;
  }
}
