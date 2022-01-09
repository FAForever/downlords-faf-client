package com.faforever.client.fx.contextmenu;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AvatarPickerMenuItemController extends AbstractCustomMenuItemController<PlayerBean> {

  private final AvatarService avatarService;
  private final I18n i18n;

  public ComboBox<AvatarBean> avatarComboBox;

  @Override
  public void afterSetObject() {
    if (getObject().getSocialStatus() != SELF) {
      getRoot().setVisible(false);
    } else {
      loadAvailableAvatars();
    }
  }

  private void loadAvailableAvatars() {
    PlayerBean player = getObject();
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      AvatarBean noAvatar = new AvatarBean();
      noAvatar.setDescription(i18n.get("chat.userContext.noAvatar"));
      items.add(0, noAvatar);

      AvatarBean currentAvatar = player.getAvatar();
      JavaFxUtil.runLater(() -> {
        avatarComboBox.getItems().setAll(items);
        avatarComboBox.getSelectionModel().select(items.stream()
            .filter(avatarBean -> Objects.equals(avatarBean, currentAvatar))
            .findFirst()
            .orElse(null));

        // Only after the box has been populated, and we selected the current value, we add the listener.
        // Otherwise, the code above already triggers a changeAvatar()
        avatarComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
          player.setAvatar(newValue);
          avatarService.changeAvatar(Objects.requireNonNullElse(newValue, noAvatar));
        });
        getRoot().setVisible(!avatarComboBox.getItems().isEmpty());
      });
    });
  }
}
