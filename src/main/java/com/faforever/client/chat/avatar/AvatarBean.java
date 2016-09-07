package com.faforever.client.chat.avatar;

import com.faforever.client.remote.domain.Avatar;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

import static com.github.nocatch.NoCatch.noCatch;

public class AvatarBean {
  private final ObjectProperty<URL> url;
  private final StringProperty description;

  public AvatarBean(@Nullable URL url, @Nullable String description) {
    this.url = new SimpleObjectProperty<>(url);
    this.description = new SimpleStringProperty(description);
  }

  public static AvatarBean fromAvatar(Avatar avatar) {
    return new AvatarBean(noCatch(() -> new URL(avatar.getUrl())), avatar.getTooltip());
  }

  @Nullable
  public URL getUrl() {
    return url.get();
  }

  public void setUrl(URL url) {
    this.url.set(url);
  }

  public ObjectProperty<URL> urlProperty() {
    return url;
  }

  @Nullable
  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }
}
