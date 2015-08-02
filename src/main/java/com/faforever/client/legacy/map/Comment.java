package com.faforever.client.legacy.map;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Comment {

  private String author;
  private String text;
  private LocalDate date;

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }


  public String getAuthor() {
    return author;
  }

  public String getText() {
    return text;
  }

  public LocalDate getDate() {
    return date;
  }
}
