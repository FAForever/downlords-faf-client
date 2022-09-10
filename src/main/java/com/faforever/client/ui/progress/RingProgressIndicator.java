/* 
 * Copyright (c) 2014, Andrea Vacondio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.faforever.client.ui.progress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.util.StringConverter;


/**
 * Progress indicator showing a filling arc.
 * 
 * @author Andrea Vacondio
 *
 */
public class RingProgressIndicator extends ProgressCircleIndicator {
    public ObjectProperty<StringConverter<Integer>> progressLableStringConverter = new SimpleObjectProperty<>(new StringConverter<>() {
        @Override
        public String toString(Integer object) {
            return String.format("%d%%", object);
        }

        @Override
        public Integer fromString(String string) {
            throw new UnsupportedOperationException();
        }
    });

    public RingProgressIndicator() {
        this.getStyleClass().add("ringindicator");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new RingProgressIndicatorSkin(this);
    }

    public final void setRingWidth(int value) {
        ringWidthProperty().set(value);
    }

    public final DoubleProperty ringWidthProperty() {
        return ringWidth;
    }

    public final double getRingWidth() {
        return ringWidthProperty().get();
    }

    /**
     * thickness of the ring indicator.
     */
    private DoubleProperty ringWidth = new StyleableDoubleProperty(22) {
        @Override
        public Object getBean() {
            return RingProgressIndicator.this;
        }

        @Override
        public String getName() {
            return "ringWidth";
        }

        @Override
        public CssMetaData<RingProgressIndicator, Number> getCssMetaData() {
            return StyleableProperties.RING_WIDTH;
        }
    };

    private static class StyleableProperties {
        private static final CssMetaData<RingProgressIndicator, Number> RING_WIDTH = new CssMetaData<RingProgressIndicator, Number>(
                "-fx-ring-width", SizeConverter.getInstance(), 22) {

            @Override
            public boolean isSettable(RingProgressIndicator n) {
                return n.ringWidth == null || !n.ringWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(RingProgressIndicator n) {
                return (StyleableProperty<Number>) n.ringWidth;
            }
        };

        public static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            styleables.addAll(ProgressCircleIndicator.getClassCssMetaData());
            styleables.add(RING_WIDTH);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
    	 return StyleableProperties.STYLEABLES;
    }

    public StringConverter<Integer> getProgressLableStringConverter() {
        return progressLableStringConverter.get();
    }

    public ObjectProperty<StringConverter<Integer>> progressLableStringConverterProperty() {
        return progressLableStringConverter;
    }

    public void setProgressLableStringConverter(StringConverter<Integer> progressLableStringConverter) {
        this.progressLableStringConverter.set(progressLableStringConverter);
    }
}