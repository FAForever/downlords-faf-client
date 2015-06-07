package com.faforever.client.leaderboard;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class LadderServiceImpl implements LadderService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void getLadderInfo(Callback<List<LadderEntryBean>> callback) {
    serverAccessor.requestLadderInfoInBackground(callback);
  }
}
