package deneme.Interfaces;

import java.awt.Color;

import deneme.GLCore.IDLabel;

public interface IIDBuilder {
	IIDBuilder size(int px);
	IIDBuilder textColor(Color color);

	/** ID etiketi mark'tan ayri bir sinif (IDLabel) olarak cizilir. */
	IDLabel build();
}
