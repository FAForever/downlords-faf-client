package com.faforever.client.news;

import java.util.Date;

public class NewsItem {

  private String author;
  private String link;
  private String title;
  private String content;
  private Date date;

  public NewsItem(String author, String link, String title, String content, Date date) {
    this.author = author;
    this.link = link;
    this.title = title;
    this.content = content;
    this.date = date;
  }

  public String getAuthor() {
    return author;
  }

  public String getLink() {
    return link;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public Date getDate() {
    return date;
  }
}
