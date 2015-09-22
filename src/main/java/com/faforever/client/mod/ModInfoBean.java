package com.faforever.client.mod;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.Objects;

public class ModInfoBean {

  private final StringProperty name;
  private final ObjectProperty<Path> imagePath;
  private final StringProperty uid;
  private final StringProperty description;
  private final StringProperty author;
  private final BooleanProperty selectable;
  private final BooleanProperty uiOnly;
  private final StringProperty version;
  private final BooleanProperty selected;

  public ModInfoBean() {
    name = new SimpleStringProperty();
    imagePath = new SimpleObjectProperty<>();
    uid = new SimpleStringProperty();
    description = new SimpleStringProperty();
    author = new SimpleStringProperty();
    selectable = new SimpleBooleanProperty();
    uiOnly = new SimpleBooleanProperty();
    version = new SimpleStringProperty();
    selected = new SimpleBooleanProperty();
  }

  public boolean getSelected() {
    return selected.get();
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }

  public String getAuthor() {
    return author.get();
  }

  public void setAuthor(String author) {
    this.author.set(author);
  }

  public StringProperty authorProperty() {
    return author;
  }

  public boolean getSelectable() {
    return selectable.get();
  }

  public void setSelectable(boolean selectable) {
    this.selectable.set(selectable);
  }

  public BooleanProperty selectableProperty() {
    return selectable;
  }

  public boolean getUiOnly() {
    return uiOnly.get();
  }

  public void setUiOnly(boolean uiOnly) {
    this.uiOnly.set(uiOnly);
  }

  public BooleanProperty uiOnlyProperty() {
    return uiOnly;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public String getVersion() {
    return version.get();
  }

  public void setVersion(String version) {
    this.version.set(version);
  }

  public StringProperty versionProperty() {
    return version;
  }

  public Path getImagePath() {
    return imagePath.get();
  }

  public void setImagePath(Path imagePath) {
    this.imagePath.set(imagePath);
  }

  public ObjectProperty<Path> imagePathProperty() {
    return imagePath;
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getUid() {
    return uid.get();
  }

  public void setUid(String uid) {
    this.uid.set(uid);
  }

  public StringProperty uidProperty() {
    return uid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid.get());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModInfoBean that = (ModInfoBean) o;
    return Objects.equals(uid.get(), that.uid.get());
  }
}
