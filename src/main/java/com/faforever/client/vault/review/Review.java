package com.faforever.client.vault.review;

import com.faforever.client.player.Player;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Review {
  private final ObjectProperty<Integer> id;
  private final StringProperty text;
  private final ObjectProperty<Player> player;
  private final IntegerProperty score;

  public Review() {
    id = new SimpleObjectProperty<>();
    text = new SimpleStringProperty();
    score = new SimpleIntegerProperty();
    player = new SimpleObjectProperty<>();
  }

  public static Review fromDto(com.faforever.client.api.dto.Review dto) {
    Review review = new Review();
    review.setId(Integer.parseInt(dto.getId()));
    review.setText(dto.getText());
    review.setScore(dto.getScore());
    review.setPlayer(Player.fromDto(dto.getPlayer()));
    return review;
  }

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public String getText() {
    return text.get();
  }

  public void setText(String text) {
    this.text.set(text);
  }

  public StringProperty textProperty() {
    return text;
  }

  public Player getPlayer() {
    return player.get();
  }

  public void setPlayer(Player player) {
    this.player.set(player);
  }

  public ObjectProperty<Player> playerProperty() {
    return player;
  }

  public int getScore() {
    return score.get();
  }

  public void setScore(int score) {
    this.score.set(score);
  }

  public IntegerProperty scoreProperty() {
    return score;
  }
}
