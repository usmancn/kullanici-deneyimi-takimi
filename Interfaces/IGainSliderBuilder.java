package deneme.Interfaces;

import java.awt.Color;

import deneme.App.GainFilterSlider;

public interface IGainSliderBuilder {

    IGainSliderBuilder backgroundColor(Color color);
    IGainSliderBuilder trackBackgroundColor(Color color);
    IGainSliderBuilder trackRangeColor(Color color);
    IGainSliderBuilder thumbColor(Color color);
    IGainSliderBuilder thumbBorderColor(Color color);
    IGainSliderBuilder tickColor(Color color);
    IGainSliderBuilder labelColor(Color color);
    IGainSliderBuilder titleColor(Color color);
    IGainSliderBuilder valueColor(Color color);
    IGainSliderBuilder hasTitle(boolean hasTitle);
    IGainSliderBuilder hasSecondTitle(boolean hasSecondTitle);

    GainFilterSlider build();
}