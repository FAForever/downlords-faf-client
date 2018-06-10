package com.faforever.client.news;

import lombok.Data;

import java.util.Date;

@Data
class NewsItem {

  private final String author;
  private final String link;
  private final String title;
  private final String content;
  private final Date date;
  private final NewsCategory newsCategory;
}
