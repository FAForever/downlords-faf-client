package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.util.TimeService;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModCardController implements Controller<Node> {

  private final ModService modService;
  private final TimeService timeService;
  public Label commentsLabel;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Label likesLabel;
  public Node modTileRoot;
  public Label updatedDateLabel;
  private Mod mod;
  private Consumer<Mod> onOpenDetailListener;
  private ListChangeListener<Mod> installStatusChangeListener;

  @Inject
  public ModCardController(ModService modService, TimeService timeService) {
    this.modService = modService;
    this.timeService = timeService;
  }

  public void initialize() {
    installStatusChangeListener = change -> {
      while (change.next()) {
        for (Mod mod : change.getAddedSubList()) {
          if (this.mod.getUid().equals(mod.getUid())) {
            setInstalled(true);
            return;
          }
        }
        for (Mod mod : change.getRemoved()) {
          if (this.mod.getUid().equals(mod.getUid())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
  }

  private void setInstalled(boolean installed) {

  }

  public void setMod(Mod mod) {
    this.mod = mod;
    thumbnailImageView.setImage(modService.loadThumbnail(mod));
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());
    likesLabel.setText(String.format("%d", mod.getLikes()));
    commentsLabel.setText(String.format("%d", mod.getComments().size()));
    updatedDateLabel.setText(timeService.asDate(mod.getPublishDate()));

    ObservableList<Mod> installedMods = modService.getInstalledMods();
    synchronized (installedMods) {
      installedMods.addListener(new WeakListChangeListener<>(installStatusChangeListener));
    }
  }

  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<Mod> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(mod);
  }
}
