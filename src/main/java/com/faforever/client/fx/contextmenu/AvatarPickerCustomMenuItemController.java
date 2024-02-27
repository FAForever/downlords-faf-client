package com.faforever.client.fx.contextmenu;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AvatarPickerCustomMenuItemController extends AbstractCustomMenuItemController<PlayerBean> {

  private final AvatarService avatarService;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final SimpleInvalidationListener selectedItemPropertyListener = this::setAvatar;

  public ComboBox<AvatarBean> avatarComboBox;

  private AvatarBean noAvatar;

  @Override
  public void afterSetObject() {
    if (object != null && object.getSocialStatus() == SELF) {
      avatarComboBox.setCellFactory(param -> avatarCell());
      avatarComboBox.setButtonCell(avatarCell());

      noAvatar = new AvatarBean(null, null, i18n.get("chat.userContext.noAvatar"));
      loadAvailableAvatars();
    }
  }

  private StringListCell<AvatarBean> avatarCell() {
    return new StringListCell<>(AvatarBean::description,
        avatarBean -> new ImageView(avatarService.loadAvatar(avatarBean)), fxApplicationThreadExecutor);
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && object.getSocialStatus() == SELF &&
        avatarComboBox.getItems().size() > 1 && avatarComboBox.getItems().getFirst().equals(noAvatar);
  }

  private void loadAvailableAvatars() {
    avatarService.getAvailableAvatars().thenAcceptAsync(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      items.addFirst(noAvatar);

      AvatarBean currentAvatar = object.getAvatar();
      avatarComboBox.getItems().setAll(items);
      avatarComboBox.getSelectionModel().select(items.stream()
          .filter(avatarBean -> Objects.equals(avatarBean, currentAvatar))
          .findFirst()
          .orElse(null));

      // Only after the box has been populated, and we selected the current value, we add the listener.
      // Otherwise, the code above already triggers a changeAvatar()
      JavaFxUtil.addListener(avatarComboBox.getSelectionModel()
          .selectedItemProperty(), new WeakInvalidationListener(selectedItemPropertyListener));
      getRoot().setVisible(isItemVisible());
    }, fxApplicationThreadExecutor);
  }

  private void setAvatar() {
    AvatarBean selectedAvatar = avatarComboBox.getSelectionModel().getSelectedItem();
    object.setAvatar(selectedAvatar);
    avatarService.changeAvatar(Objects.requireNonNullElse(selectedAvatar, noAvatar));
  }
}
