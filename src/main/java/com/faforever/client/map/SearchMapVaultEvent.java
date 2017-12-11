package com.faforever.client.map;

import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.vault.search.SearchController.SearchConfig;

public class SearchMapVaultEvent extends NavigateEvent {
  SearchConfig searchConfig;

  public SearchMapVaultEvent() {
    super(NavigationItem.VAULT);
  }
}
