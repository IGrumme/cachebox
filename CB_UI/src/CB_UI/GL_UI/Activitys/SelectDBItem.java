package CB_UI.GL_UI.Activitys;

import java.io.File;

import CB_UI_Base.GL_UI.Fonts;
import CB_UI_Base.GL_UI.Controls.Label;
import CB_UI_Base.GL_UI.Controls.Label.VAlignment;
import CB_UI_Base.GL_UI.Controls.List.ListViewItemBackground;
import CB_UI_Base.Math.CB_RectF;

public class SelectDBItem extends ListViewItemBackground {

    Label nameLabel;
    Label countLabel;
    protected static final float left = 20;

    protected static float mLabelHeight = -1;
    protected static float mLabelYPos = -1;
    protected static float mLabelWidth = -1;

    public SelectDBItem(CB_RectF rec, int Index, File file, String count) {
	super(rec, Index, file.getName());

	if (mLabelHeight == -1) {
	    mLabelHeight = getHeight() * 0.7f;
	    mLabelYPos = (getHeight() - mLabelHeight) / 2;
	    mLabelWidth = getWidth() - (left * 2);
	}

	nameLabel = new Label(this.name + " nameLabel", left, mLabelYPos, getWidth(), mLabelHeight);
	nameLabel.setFont(Fonts.getBig());
	nameLabel.setVAlignment(VAlignment.TOP);
	nameLabel.setText(file.getName());
	this.addChild(nameLabel);

	countLabel = new Label(this.name + " countLabel", left, mLabelYPos, getWidth(), mLabelHeight);
	countLabel.setFont(Fonts.getBubbleNormal());
	countLabel.setVAlignment(VAlignment.BOTTOM);
	countLabel.setText(count);
	this.addChild(countLabel);

	this.setClickable(true);
    }

    // @Override
    // public boolean onTouchDown(int x, int y, int pointer, int button)
    // {
    // return false;
    // }
    //
    // @Override
    // public boolean onTouchDragged(int x, int y, int pointer, boolean KineticPan)
    // {
    // return false;
    // }
    //
    // @Override
    // public boolean onTouchUp(int x, int y, int pointer, int button)
    // {
    // return true;
    // }

    @Override
    protected void SkinIsChanged() {

    }

}
