package deneme.Interfaces;

import java.awt.Color;

import deneme.App.MarkMenuStyle;

public interface IMarkMenuRenderer {
	IMarkMenuRenderer labelColor(Color color);
	IMarkMenuRenderer backgroundColor(Color color);

	MarkMenuStyle build();
}
