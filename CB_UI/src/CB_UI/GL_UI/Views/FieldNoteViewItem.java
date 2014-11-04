package CB_UI.GL_UI.Views;

import java.text.SimpleDateFormat;

import CB_Core.Enums.LogTypes;
import CB_Core.Types.FieldNoteEntry;
import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI.Tag;
import CB_UI_Base.GL_UI.Fonts;
import CB_UI_Base.GL_UI.GL_View_Base;
import CB_UI_Base.GL_UI.SpriteCacheBase;
import CB_UI_Base.GL_UI.SpriteCacheBase.IconName;
import CB_UI_Base.GL_UI.Controls.Button;
import CB_UI_Base.GL_UI.Controls.Image;
import CB_UI_Base.GL_UI.Controls.Label;
import CB_UI_Base.GL_UI.Controls.List.ListViewItemBackground;
import CB_UI_Base.Math.CB_RectF;
import CB_UI_Base.Math.UI_Size_Base;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;

public class FieldNoteViewItem extends ListViewItemBackground
{
	private static NinePatch backheader;
	private FieldNoteEntry fieldnote;
	private Image ivTyp;
	private Label lblDate;
	private Image ivCacheType;
	private Label lblCacheName;
	private Label lblGcCode;
	private Label lblComment;

	private static float MeasuredLabelHeight = 0;

	public FieldNoteViewItem(CB_RectF rec, int Index, FieldNoteEntry fieldnote)
	{
		super(rec, Index, "");

		this.fieldnote = fieldnote;
		mBackIsInitial = false;
		MeasuredLabelHeight = Fonts.Measure("T").height * 1.5f;
		headHeight = (UI_Size_Base.that.getButtonHeight() / 1.5f) + (UI_Size_Base.that.getMargin());

		iniImage();
		iniDateLabel();
		iniCacheTypeImage();
		iniCacheNameLabel();
		iniGcCodeLabel();
		iniCommentLabel();

		if (this.fieldnote == null)
		{
			Button btnLoadMore = new Button(Translation.Get("LoadMore"));
			btnLoadMore.setWidth(this.getWidth());
			btnLoadMore.setOnClickListener(new OnClickListener()
			{

				@Override
				public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
				{
					if (FieldNoteViewItem.this.mOnClickListener != null) FieldNoteViewItem.this.mOnClickListener.onClick(v, x, y, pointer, button);
					return true;
				}
			});
			this.addChild(btnLoadMore);
		}
	}

	private void iniImage()
	{
		if (this.fieldnote == null) return;
		ivTyp = new Image(getLeftWidth(), this.getHeight() - (headHeight / 2) - (UI_Size_Base.that.getButtonHeight() / 1.5f / 2), UI_Size_Base.that.getButtonHeight() / 1.5f, UI_Size_Base.that.getButtonHeight() / 1.5f, "");
		this.addChild(ivTyp);
		ivTyp.setDrawable(getTypeIcon(this.fieldnote));
	}

	public static Drawable getTypeIcon(FieldNoteEntry fne)
	{
		LogTypes type = fne.type;

		if (fne.isTbFieldNote)
		{

			Sprite spr = null;

			if (type == LogTypes.discovered) spr = SpriteCacheBase.Icons.get(IconName.tbDiscover_58.ordinal());
			if (type == LogTypes.dropped_off) spr = SpriteCacheBase.Icons.get(IconName.tbDrop_59.ordinal());
			if (type == LogTypes.grab_it) spr = SpriteCacheBase.Icons.get(IconName.tbGrab_60.ordinal());
			if (type == LogTypes.retrieve) spr = SpriteCacheBase.Icons.get(IconName.tbPicked_61.ordinal());
			if (type == LogTypes.visited) spr = SpriteCacheBase.Icons.get(IconName.tbVisit_62.ordinal());
			if (type == LogTypes.note) spr = SpriteCacheBase.Icons.get(IconName.tbNote_63.ordinal());
			if (spr == null) return null;
			return new SpriteDrawable(spr);
		}
		else
		{
			return new SpriteDrawable(SpriteCacheBase.LogIcons.get(fne.typeIcon));
		}
	}

	private void iniDateLabel()
	{
		if (this.fieldnote == null) return;
		// SimpleDateFormat postFormater = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
		SimpleDateFormat postFormater = new SimpleDateFormat("dd.MMM (HH:mm)");
		String foundNumber = "";
		if (fieldnote.foundNumber > 0)
		{
			foundNumber = "#" + fieldnote.foundNumber + " @ ";
		}
		String dateString = foundNumber + postFormater.format(fieldnote.timestamp);
		float DateLength = 100;

		try
		{
			DateLength = Fonts.Measure(dateString).width;
		}
		catch (Exception e)
		{

			Gdx.app.error(Tag.TAG, "", e);
		}

		lblDate = new Label(this.getWidth() - getRightWidth() - DateLength, this.getHeight() - (headHeight / 2) - (MeasuredLabelHeight / 2), DateLength, MeasuredLabelHeight, "");
		lblDate.setFont(Fonts.getNormal());
		lblDate.setText(dateString);
		this.addChild(lblDate);
	}

	private void iniCacheTypeImage()
	{
		if (this.fieldnote == null) return;
		ivCacheType = new Image(getLeftWidth() + UI_Size_Base.that.getMargin(), this.getHeight() - headHeight - (UI_Size_Base.that.getButtonHeight()) - UI_Size_Base.that.getMargin(), UI_Size_Base.that.getButtonHeight(), UI_Size_Base.that.getButtonHeight(), "");
		this.addChild(ivCacheType);

		if (fieldnote.isTbFieldNote)
		{
			ivCacheType.setImageURL(fieldnote.TbIconUrl);
		}
		else
		{
			ivCacheType.setDrawable(new SpriteDrawable(SpriteCacheBase.BigIcons.get(fieldnote.cacheType)));
		}
	}

	private void iniCacheNameLabel()
	{
		if (this.fieldnote == null) return;
		lblCacheName = new Label(ivCacheType.getMaxX() + UI_Size_Base.that.getMargin(), this.getHeight() - headHeight - MeasuredLabelHeight - UI_Size_Base.that.getMargin(), this.getWidth() - ivCacheType.getMaxX() - (UI_Size_Base.that.getMargin() * 2), MeasuredLabelHeight, "");
		lblCacheName.setFont(Fonts.getNormal());
		lblCacheName.setText(fieldnote.isTbFieldNote ? fieldnote.TbName : fieldnote.CacheName);
		this.addChild(lblCacheName);

	}

	private void iniGcCodeLabel()
	{
		if (this.fieldnote == null) return;
		lblGcCode = new Label(lblCacheName.getX(), lblCacheName.getY() - MeasuredLabelHeight - UI_Size_Base.that.getMargin(), this.getWidth() - ivCacheType.getMaxX() - (UI_Size_Base.that.getMargin() * 2), MeasuredLabelHeight, "");
		lblGcCode.setFont(Fonts.getNormal());
		lblGcCode.setText(fieldnote.gcCode);
		this.addChild(lblGcCode);

	}

	private void iniCommentLabel()
	{
		if (this.fieldnote == null) return;
		lblComment = new Label(getLeftWidth() + UI_Size_Base.that.getMargin(), 0, this.getWidth() - getLeftWidth() - getRightWidth() - (UI_Size_Base.that.getMargin() * 2), this.getHeight() - (this.getHeight() - lblGcCode.getY()) - UI_Size_Base.that.getMargin(), "");
		lblComment.setFont(Fonts.getNormal());
		lblComment.setWrappedText(fieldnote.comment);
		this.addChild(lblComment);

	}

	@Override
	protected void Initial()
	{
		backheader = new NinePatch(SpriteCacheBase.getThemedSprite("listrec-header"), 8, 8, 8, 8);
		super.Initial();
	}

	// // static Member
	// public static Paint Linepaint;
	// public static Paint KopfPaint;
	// public static Paint TextPaint;
	private static float headHeight;

	public static BitmapFontCache cacheNamePaint;

	@Override
	protected void SkinIsChanged()
	{

	}

	@Override
	public void render(Batch batch)
	{
		if (fieldnote == null)
		{
			// super.render(batch);
			return;
		}

		Color color = batch.getColor();
		float oldAlpha = color.a;
		float oldRed = color.r;
		float oldGreen = color.g;
		float oldBlue = color.b;

		boolean uploaded = false;
		if (fieldnote.uploaded) uploaded = true;

		if (uploaded)
		{
			color.a = 0.5f;
			color.r = 0.6f;
			color.g = 0.65f;
			color.b = 0.6f;
			batch.setColor(color);
		}

		super.render(batch);
		if (backheader != null)
		{
			backheader.draw(batch, 0, this.getHeight() - headHeight, this.getWidth(), headHeight);
		}
		else
		{
			resetInitial();
		}

		if (uploaded)
		{
			ivTyp.setColor(color);
			ivCacheType.setColor(color);

			color.a = oldAlpha;
			color.r = oldRed;
			color.g = oldGreen;
			color.b = oldBlue;
			batch.setColor(color);
		}

	}
}
