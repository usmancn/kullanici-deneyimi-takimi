package deneme.Builder;

import java.awt.Color;

import deneme.App.GainFilterSlider;
import deneme.Controller.GainFilterController;
import deneme.Interfaces.IGainSliderBuilder;

public class GainSliderBuilder implements IGainSliderBuilder {

	private final GainFilterSlider slider = new GainFilterSlider();
	private boolean controllerInstalled = false;

	@Override
	public IGainSliderBuilder backgroundColor(Color color) {
		slider.setBackgroundColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder trackBackgroundColor(Color color) {
		slider.setTrackBackgroundColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder trackRangeColor(Color color) {
		slider.setTrackRangeColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder thumbColor(Color color) {
		slider.setThumbColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder thumbBorderColor(Color color) {
		slider.setThumbBorderColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder tickColor(Color color) {
		slider.setTickColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder labelColor(Color color) {
		slider.setLabelColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder titleColor(Color color) {
		slider.setTitleColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder valueColor(Color color) {
		slider.setValueColor(color);
		return this;
	}

	@Override
	public IGainSliderBuilder hasTitle(boolean hasTitle) {
		slider.setHasTitle(hasTitle);
		return this;
	}

	@Override
	public IGainSliderBuilder hasSecondTitle(boolean hasSecondTitle) {
		slider.setHasSecondTitle(hasSecondTitle);
		return this;
	}

	@Override
	public GainFilterSlider build() {
		// fare kontrolu slider'in icinde degil, controller'da (bir kez kurulur)
		if (!controllerInstalled) {
			new GainFilterController(slider).install();
			controllerInstalled = true;
		}
		return slider;
	}
}
