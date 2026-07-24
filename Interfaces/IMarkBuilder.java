package deneme.Interfaces;

import java.awt.Color;

import deneme.GLCore.MarkStyle;
import deneme.GLCore.MarkType;

public interface IMarkBuilder {
	IMarkBuilder markColor(Color color);
	IMarkBuilder size(int size);
	IMarkBuilder type(MarkType type);   // kare / daire / ucgen

	/** Mark kayitlari (ID -> Mark) ayri durur; builder cizim stilini uretir. */
	MarkStyle build();
}
