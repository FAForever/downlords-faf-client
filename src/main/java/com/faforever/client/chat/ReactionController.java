package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReactionController extends NodeController<HBox> {

  private static final String MY_REACTION_CLASS = "my-reaction";

  private final EmoticonService emoticonService;
  private final ChatService chatService;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Label label;
  public HBox root;
  public ImageView emoticonImageView;

  private final ObjectProperty<Emoticon> reaction = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableMap<String, String>> reactors = new SimpleObjectProperty<>();
  private final ObjectProperty<Consumer<Emoticon>> onReactionClicked = new SimpleObjectProperty<>();

  private final MapChangeListener<String, String> reactorsListener = this::onReactorsChanged;

  private final Tooltip reactorsTooltip = new Tooltip();

  @Override
  public void onInitialize() {
    root.onMouseClickedProperty()
        .bind(onReactionClicked.flatMap(onReactionClicked -> reaction.map(
            reaction -> (EventHandler<MouseEvent>) event -> onReactionClicked.accept(reaction))).when(showing));

    emoticonImageView.imageProperty()
                     .bind(reaction.map(Emoticon::shortcodes)
                                   .map(SequencedCollection::getFirst)
                                   .map(emoticonService::getImageByShortcode)
                                   .when(showing));

    reactors.subscribe((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeListener(reactorsListener);
      }

      if (newValue != null) {
        updateReactors(newValue);
        newValue.addListener(reactorsListener);
      }
    });

    reactorsTooltip.setFont(new Font(14));
    reactorsTooltip.setShowDuration(Duration.seconds(10));
    reactorsTooltip.setShowDelay(Duration.ZERO);
    reactorsTooltip.setHideDelay(Duration.ZERO);
    Tooltip.install(root, reactorsTooltip);
  }

  private void onReactorsChanged(MapChangeListener.Change<? extends String, ? extends String> change) {
    updateReactors(change.getMap());
  }

  private void updateReactors(Map<? extends String, ? extends String> map) {
    boolean selected = map.containsKey(chatService.getCurrentUsername());
    Set<? extends String> reactors = Set.copyOf(map.keySet());
    fxApplicationThreadExecutor.execute(() -> {
      ObservableList<String> styleClass = root.getStyleClass();
      if (selected && !styleClass.contains(MY_REACTION_CLASS)) {
        styleClass.add(MY_REACTION_CLASS);
      } else if (!selected) {
        styleClass.removeIf(MY_REACTION_CLASS::equals);
      }
      reactorsTooltip.setText(String.join(", ", reactors));
      label.setText(i18n.number(reactors.size()));
    });
  }

  @Override
  public HBox getRoot() {
    return root;
  }

  public Emoticon getReaction() {
    return reaction.get();
  }

  public ObjectProperty<Emoticon> reactionProperty() {
    return reaction;
  }

  public void setReaction(Emoticon reaction) {
    this.reaction.set(reaction);
  }

  public ObservableMap<String, String> getReactors() {
    return reactors.get();
  }

  public ObjectProperty<ObservableMap<String, String>> reactorsProperty() {
    return reactors;
  }

  public void setReactors(ObservableMap<String, String> reactors) {
    this.reactors.set(reactors);
  }

  public Consumer<Emoticon> getOnReactionClicked() {
    return onReactionClicked.get();
  }

  public ObjectProperty<Consumer<Emoticon>> onReactionClickedProperty() {
    return onReactionClicked;
  }

  public void setOnReactionClicked(Consumer<Emoticon> onReactionClicked) {
    this.onReactionClicked.set(onReactionClicked);
  }
}
