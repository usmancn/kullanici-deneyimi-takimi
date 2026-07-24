package deneme.Interfaces;

import java.awt.Color;

import deneme.GLCore.Minimap;

public interface IMinimapBuilder {
	IMinimapBuilder CanClosable(boolean CanClosable);
	IMinimapBuilder Fraction(double fraction);
	IMinimapBuilder borderColor(Color color);
	IMinimapBuilder squareColor(Color color);
	
	Minimap build();
}
