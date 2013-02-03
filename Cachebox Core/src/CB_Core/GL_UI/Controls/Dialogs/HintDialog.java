package CB_Core.GL_UI.Controls.Dialogs;

import CB_Core.GlobalCore;
import CB_Core.GL_UI.GL_View_Base;
import CB_Core.GL_UI.Controls.Label;
import CB_Core.GL_UI.Controls.ScrollBox;
import CB_Core.GL_UI.Controls.MessageBox.GL_MsgBox;
import CB_Core.GL_UI.Controls.MessageBox.MessageBoxButtons;
import CB_Core.GL_UI.GL_Listener.GL;
import CB_Core.Math.Size;
import CB_Core.TranslationEngine.Translation;

import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

public class HintDialog extends GL_MsgBox
{

	static ScrollBox scrollBox;
	static String hintTextDecoded;
	static String hintTextEncoded;

	public HintDialog(Size size, String name)
	{
		super(size, name);

	}

	/**
	 * Zeigt den Hint des Global angewählten Caches
	 */
	public static void show()
	{
		if (GlobalCore.getSelectedCache() == null) return;
		hintTextDecoded = GlobalCore.Rot13(GlobalCore.getSelectedCache().hint);
		hintTextEncoded = GlobalCore.getSelectedCache().hint;

		Size decodedSize = calcMsgBoxSize(hintTextDecoded, true, true, false);
		Size encodedSize = calcMsgBoxSize(hintTextEncoded, true, true, false);

		Size maxTextSize = decodedSize.height > encodedSize.height ? decodedSize : encodedSize;

		GL_MsgBox msgBox = new GL_MsgBox(maxTextSize, "MsgBox");
		msgBox.setTitle(Translation.Get("hint"));
		msgBox.setButtonCaptions(MessageBoxButtons.OKCancel);
		msgBox.mMsgBoxClickListner = null;

		scrollBox = new ScrollBox(msgBox.getContentSize().getBounds(), 100, "");

		msgBox.label = new Label(msgBox.getContentSize().getBounds(), "MsgBoxLabel");
		msgBox.label.setZeroPos();
		msgBox.label.setWrappedText(hintTextDecoded);
		TextBounds bounds = msgBox.label.bounds;
		msgBox.label.setHeight(bounds.height);

		// label in Scrollbox verpacken
		scrollBox.addChild(msgBox.label);
		scrollBox.setInerHeight(msgBox.label.getHeight());

		msgBox.addChild(scrollBox);

		msgBox.button3.setText(Translation.Get("close"));
		msgBox.button1.setText(Translation.Get("decode"));

		msgBox.button1.setOnClickListener(new OnClickListener()
		{

			@Override
			public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
			{
				GL_MsgBox msgBox = (GL_MsgBox) GL.that.getActDialog();
				msgBox.setText(GlobalCore.Rot13(msgBox.label.getText())); // toggle
				TextBounds bounds = msgBox.label.bounds;
				msgBox.label.setHeight(bounds.height);
				scrollBox.setInerHeight(msgBox.label.getHeight());
				return true;
			}
		});

		GL.that.showDialog(msgBox);

	}
}
