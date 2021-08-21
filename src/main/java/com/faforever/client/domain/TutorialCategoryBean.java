package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class TutorialCategoryBean {

  @EqualsAndHashCode.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  StringProperty categoryKey = new SimpleStringProperty();
  StringProperty category = new SimpleStringProperty();
  ObservableList<TutorialBean> tutorials = FXCollections.observableArrayList();

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public String getCategoryKey() {
    return categoryKey.get();
  }

  public void setCategoryKey(String categoryKey) {
    this.categoryKey.set(categoryKey);
  }

  public StringProperty categoryKeyProperty() {
    return categoryKey;
  }

  public String getCategory() {
    return category.get();
  }

  public void setCategory(String category) {
    this.category.set(category);
  }

  public StringProperty categoryProperty() {
    return category;
  }

  public ObservableList<TutorialBean> getTutorials() {
    return tutorials;
  }

  public void setTutorials(List<TutorialBean> tutorials) {
    if (tutorials == null) {
      tutorials = List.of();
    }
    this.tutorials.setAll(tutorials);
  }
}
