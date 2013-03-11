package CB_Core.GL_UI.Controls;

import java.text.NumberFormat;

import CB_Core.Config;
import CB_Core.Events.invalidateTextureEvent;
import CB_Core.Events.invalidateTextureEventList;
import CB_Core.GL_UI.CB_View_Base;
import CB_Core.GL_UI.Fonts;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.Views.MapView;
import CB_Core.Log.Logger;
import CB_Core.Math.CB_RectF;

import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class MapScale extends CB_View_Base implements invalidateTextureEvent
{
	private BitmapFontCache fontCache;
	private float sollwidth = 0;
	private MapView mapInstanz;
	private Drawable CachedScaleSprite;
	private float drawableWidth = 0;
	private String distanceString;

	public MapScale(CB_RectF rec, String Name, MapView mapInstanz)
	{
		super(rec, Name);
		sollwidth = rec.getWidth();
		this.mapInstanz = mapInstanz;
		CachedScaleSprite = null;
		fontCache = new BitmapFontCache(Fonts.getNormal());
		fontCache.setColor(Fonts.getFontColor());
		invalidateTextureEventList.Add(this);
	}

	@Override
	protected void Initial()
	{
		generatedZomm = -1;
		zoomChanged();
	}

	@Override
	protected void SkinIsChanged()
	{
		invalidateTexture();
		zoomChanged();
	}

	/**
	 * Anzahl der Schritte auf dem Ma�stab
	 */
	int scaleUnits = 10;

	/**
	 * L�nge des Ma�stabs in Metern
	 */
	double scaleLength = 1000;

	float pixelsPerMeter;

	int generatedZomm = -1;

	/**
	 * Nachdem Zoom ver�ndert wurde m�ssen einige Werte neu berechnet werden
	 */
	public void zoomChanged()
	{
		if (mapInstanz.pixelsPerMeter <= 0) return;
		if (mapInstanz.getAktZoom() == generatedZomm) return;

		try
		{
			int[] scaleNumUnits = new int[]
				{ 4, 3, 4, 3, 4, 5, 3 };
			float[] scaleSteps = new float[]
				{ 1, 1.5f, 2, 3, 4, 5, 7.5f };

			pixelsPerMeter = mapInstanz.pixelsPerMeter;

			int multiplyer = 1;
			double scaleSize = 0;
			int idx = 0;
			while (scaleSize < (sollwidth * 0.45))
			{
				scaleLength = multiplyer * scaleSteps[idx] * ((Config.settings.ImperialUnits.getValue()) ? 1.6093 : 1);
				scaleUnits = scaleNumUnits[idx];

				scaleSize = pixelsPerMeter * scaleLength;

				idx++;
				if (idx == scaleNumUnits.length)
				{
					idx = 0;
					multiplyer *= 10;
				}
			}
		}
		catch (Exception exc)
		{
			Logger.Error("MapView.zoomChanged()", "", exc);
		}

		if (Config.settings.ImperialUnits.getValue())
		{
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(2);
			distanceString = nf.format(scaleLength / 1609.3) + "mi";
		}
		else if (scaleLength <= 500)
		{
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(0);
			distanceString = nf.format(scaleLength) + "m";
		}
		else
		{
			double length = scaleLength / 1000;
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(0);
			distanceString = nf.format(length) + "km";
		}

		ZoomChanged();
	}

	public void ZoomChanged()
	{
		pixelsPerMeter = mapInstanz.pixelsPerMeter;
		drawableWidth = (int) (scaleLength * pixelsPerMeter);
		if (fontCache == null)
		{
			fontCache = new BitmapFontCache(Fonts.getNormal());
			fontCache.setColor(Fonts.getFontColor());
		}

		try
		{
			TextBounds bounds = fontCache.setText(distanceString, 0, fontCache.getFont().isFlipped() ? 0 : fontCache.getFont()
					.getCapHeight());
			this.setWidth((float) (drawableWidth + (bounds.width * 1.3)));
			CachedScaleSprite = SpriteCache.MapScale[scaleUnits - 3];
			float margin = (this.height - bounds.height) / 1.6f;
			fontCache.setPosition(this.width - bounds.width - margin, margin);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Zeichnet den Ma�stab. pixelsPerKm muss durch zoomChanged initialisiert sein!
	 */
	@Override
	protected void render(SpriteBatch batch)
	{
		if (pixelsPerMeter <= 0) return;
		if (CachedScaleSprite == null) zoomChanged();
		if (CachedScaleSprite != null) CachedScaleSprite.draw(batch, 0, 0, drawableWidth, this.height);
		if (fontCache != null) fontCache.draw(batch);
	}

	@Override
	public void invalidateTexture()
	{
		if (CachedScaleSprite != null)
		{
			CachedScaleSprite = null;
		}
		generatedZomm = -1;
	}

}
