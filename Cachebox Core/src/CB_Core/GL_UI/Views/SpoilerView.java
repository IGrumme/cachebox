package CB_Core.GL_UI.Views;

import CB_Core.GlobalCore;
import CB_Core.Plattform;
import CB_Core.Events.platformConector;
import CB_Core.GL_UI.CB_View_Base;
import CB_Core.GL_UI.Fonts;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.ViewConst;
import CB_Core.GL_UI.Controls.Label;
import CB_Core.Math.CB_RectF;

public class SpoilerView extends CB_View_Base
{

	public SpoilerView(CB_RectF rec, String Name)
	{
		super(rec, Name);

		Label lblDummy = new Label(CB_RectF.ScaleCenter(rec, 0.8f), "DummyLabel");
		lblDummy.setFont(Fonts.getNormal());
		lblDummy.setText("Dummy SpoilerView");
		setBackground(SpriteCache.ListBack);

		if (GlobalCore.platform == Plattform.Desktop) this.addChild(lblDummy);

	}

	@Override
	public void onShow()
	{
		// Rufe ANDROID VIEW auf
		platformConector.showView(ViewConst.SPOILER_VIEW, this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

	@Override
	public void onHide()
	{
		platformConector.hideView(ViewConst.SPOILER_VIEW);
	}

	@Override
	protected void Initial()
	{
		 

	}

	@Override
	protected void SkinIsChanged()
	{
		 

	}

}
