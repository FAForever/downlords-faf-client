package com.faforever.client.filter;

import com.faforever.client.theme.UiService;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;

import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterTestUtil {

  public static <F extends AbstractFilterNodeController<?, ?, ?>> F mockFilter(Class<F> filterClazz, UiService uiServiceMock) {
    F filter = mock(filterClazz);
    when(uiServiceMock.loadFxml(anyString(), eq(filterClazz))).thenReturn(filter);
    when(uiServiceMock.loadFxml(anyString())).thenReturn(filter);
    when(filter.predicateProperty()).thenReturn(mock(ObjectProperty.class));
    when(filter.getPredicate()).thenReturn(mock(Predicate.class));
    Node node = null;
    if (filterClazz.isAssignableFrom(FilterCheckboxController.class)) {
      node = new CheckBox();
    } else if (filterClazz.isAssignableFrom(FilterTextFieldController.class)) {
      node = new TextField();
    } else if (filterClazz.isAssignableFrom(FilterMultiCheckboxController.class)
        || filterClazz.isAssignableFrom(MutableListFilterController.class)
        || filterClazz.isAssignableFrom(AbstractRangeSliderFilterController.class)) {
      node = new MenuButton();
    }
    when(filter.getRoot()).thenReturn(node);
    return filter;
  }
}
