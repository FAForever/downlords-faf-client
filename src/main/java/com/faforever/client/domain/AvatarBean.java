package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class AvatarBean extends AbstractEntityBean<AvatarBean> {
  @EqualsAndHashCode.Include
  ObjectProperty<URL> url = new SimpleObjectProperty<>();
  StringProperty description = new SimpleStringProperty();

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
