package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.task.PrioritizedTask;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class MapVaultParseTask extends PrioritizedTask<List<MapInfoBean>> {

  @Autowired
  I18n i18n;

  @Autowired
  MapVaultParser mapVaultParser;

  private int page;
  private int maxEntries;

  public MapVaultParseTask() {
    super(Priority.MEDIUM);
  }

  @Override
  protected List<MapInfoBean> call() throws Exception {
    updateTitle(i18n.get("readMapVaultTask.title"));

    return mapVaultParser.parseMapVault(page, maxEntries);
  }

  public void setPage(int page) {
    this.page = page;
  }

  public void setMaxEntries(int maxEntries) {
    this.maxEntries = maxEntries;
  }
}
