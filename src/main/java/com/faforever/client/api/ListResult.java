package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.util.List;

public class ListResult<T> {

  @Key("items")
  private List<T> items;

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }
}
