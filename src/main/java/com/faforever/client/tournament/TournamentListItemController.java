package com.faforever.client.tournament;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TournamentListItemController implements Controller<Node> {

  private final I18n i18n;
  private final TimeService timeService;
  public Node tournamentItemRoot;
  public ImageView imageView;
  public Label nameLabel;
  public Label statusLabel;
  public Label startingLabel;

  public TournamentListItemController(I18n i18n, TimeService timeService) {
    this.i18n = i18n;
    this.timeService = timeService;
  }

  @Override
  public Node getRoot() {
    return tournamentItemRoot;
  }

  void setTournamentItem(TournamentBean tournamentBean) {

    // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
//    imageView.setImage(uiService.getThemeImage(tournamentBean.getNewsCategory().getImagePath()));

    nameLabel.setText(tournamentBean.getName());
    if (tournamentBean.getStartingAt() == null) {
      startingLabel.setText(i18n.get("unknown"));
    } else {
      startingLabel.setText(MessageFormat.format("{0} {1}", timeService.asDate(tournamentBean.getStartingAt()), timeService.asShortTime(tournamentBean.getStartingAt())));
    }

    String statusKey;
    if (tournamentBean.getCompletedAt() != null) {
      statusKey = "tournament.status.finished";
    } else if (tournamentBean.getStartingAt() != null && tournamentBean.getStartingAt().isBefore(OffsetDateTime.now())) {
      statusKey = "tournament.status.running";
    } else if (tournamentBean.isOpenForSignup()) {
      statusKey = "tournament.status.openForRegistration";
    } else {
      statusKey = "tournament.status.closedForRegistration";
    }

    statusLabel.setText(i18n.get(statusKey));
  }
}
