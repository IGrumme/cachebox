package CB_Core.GL_UI.Activitys;

import CB_Core.GL_UI.Fonts;
import CB_Core.GL_UI.GL_View_Base;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.Controls.Dialog;
import CB_Core.GL_UI.GL_Listener.GL;
import CB_Core.Math.CB_RectF;
import CB_Core.Math.UiSizes;

public class ActivityBase extends Dialog
{
	protected ActivityBase that;
	protected float MeasuredLabelHeight;
	protected float MeasuredLabelHeightBig;
	protected float ButtonHeight;
	protected float innerWidth;

	public ActivityBase(CB_RectF rec, String Name)
	{
		super(rec, Name);
		that = this;
		dontRenderDialogBackground = true;
		this.setBackground(SpriteCache.activityBackground);

		innerWidth = this.width - this.getLeftWidth() - this.getRightWidth();
		MeasuredLabelHeight = Fonts.Measure("T").height * 1.5f;
		MeasuredLabelHeightBig = Fonts.MeasureBig("T").height * 1.5f;
		ButtonHeight = UiSizes.getButtonHeight();
	}

	@Override
	protected void SkinIsChanged()
	{
		this.setBackground(SpriteCache.activityBackground);
	}

	@Override
	public GL_View_Base addChild(GL_View_Base view)
	{
		this.addChildDirekt(view);

		return view;
	}

	public GL_View_Base addChildAtLast(GL_View_Base view)
	{
		this.addChildDirektLast(view);

		return view;
	}

	@Override
	public void removeChilds()
	{
		this.removeChildsDirekt();
	}

	@Override
	protected void Initial()
	{
		// do not call super, it wants clear childs
	}

	protected void finish()
	{
		GL.that.closeActivity();
	}

	@Override
	public void onShow()
	{
		super.onShow();
	}

	@Override
	public void onHide()
	{
		super.onHide();
	}

	public void show()
	{
		GL.that.showActivity(this);
	}

	public static CB_RectF ActivityRec()
	{
		float w = Math.min(UiSizes.getSmallestWidth(), UiSizes.getWindowHeight() * 0.66f);

		return new CB_RectF(0, 0, w, UiSizes.getWindowHeight());
	}

}
