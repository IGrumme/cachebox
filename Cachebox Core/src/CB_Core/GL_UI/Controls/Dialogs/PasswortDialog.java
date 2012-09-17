package CB_Core.GL_UI.Controls.Dialogs;

import CB_Core.GlobalCore;
import CB_Core.GL_UI.Fonts;
import CB_Core.GL_UI.Controls.EditWrapedTextField;
import CB_Core.GL_UI.Controls.Label;
import CB_Core.GL_UI.Controls.Linearlayout;
import CB_Core.GL_UI.Controls.MessageBox.ButtonDialog;
import CB_Core.GL_UI.Controls.MessageBox.GL_MsgBox;
import CB_Core.GL_UI.Controls.MessageBox.GL_MsgBox.OnMsgBoxClickListener;
import CB_Core.GL_UI.Controls.MessageBox.MessageBoxButtons;
import CB_Core.GL_UI.Controls.MessageBox.MessageBoxIcon;
import CB_Core.GL_UI.Menu.Menu;
import CB_Core.Math.CB_RectF;
import CB_Core.Math.Size;
import CB_Core.Math.SizeF;

public class PasswortDialog extends ButtonDialog
{

	public EditWrapedTextField editTextUser;
	public EditWrapedTextField editTextPW;

	private Linearlayout layout;

	private float TextFieldHeight;
	private float LabelHeight;
	private SizeF msgBoxContentSize;

	public interface returnListner
	{
		public void returnFromPW_Dialog(String User, String PW);
	}

	private returnListner mReturnListner;

	public PasswortDialog(returnListner listner)
	{
		super(Menu.MENU_REC, "PW-Dialog", "", GlobalCore.Translations.Get("enterPW"), MessageBoxButtons.OKCancel, MessageBoxIcon.GC_Live,
				null);
		mReturnListner = listner;

		msgBoxContentSize = getContentSize();
		// initial VariableField
		TextFieldHeight = Fonts.getNormal().getLineHeight() * 2.4f;
		LabelHeight = Fonts.getNormal().getLineHeight();

		layout = new Linearlayout(msgBoxContentSize.width, "Layout");

		Label lblName = new Label(0, 0, msgBoxContentSize.width, LabelHeight, "");
		lblName.setText(GlobalCore.Translations.Get("LogIn"));
		layout.addChild(lblName);

		CB_RectF rec = new CB_RectF(0, 0, msgBoxContentSize.width, TextFieldHeight);

		editTextUser = new EditWrapedTextField(this, rec, EditWrapedTextField.TextFieldType.SingleLine, "SolverDialogTextField");
		layout.addChild(editTextUser);

		Label lblPW = new Label(0, 0, msgBoxContentSize.width, LabelHeight, "");
		lblPW.setText(GlobalCore.Translations.Get("GCPW"));
		layout.addChild(lblPW);

		editTextPW = new EditWrapedTextField(this, rec, EditWrapedTextField.TextFieldType.SingleLine, "SolverDialogTextField");

		// TODO set PW-Mode => hat noch einen Fehler
		// editTextPW.setPasswordMode();
		// editTextPW.setPasswordCharacter("*".charAt(0));
		//
		layout.addChild(editTextPW);

		this.addChild(layout);

		Size msgBoxSize = GL_MsgBox.calcMsgBoxSize("teste", true, true, false);
		msgBoxSize.height = (int) (msgBoxSize.height + layout.getHeight());
		this.setSize(msgBoxSize.asFloat());

		mMsgBoxClickListner = new OnMsgBoxClickListener()
		{

			@Override
			public boolean onClick(int which)
			{
				if (which == BUTTON_POSITIVE)
				{

					if (mReturnListner != null) mReturnListner.returnFromPW_Dialog(editTextUser.getText(), editTextPW.getText());
					close();
				}
				else
				{
					if (mReturnListner != null) mReturnListner.returnFromPW_Dialog(null, null);
					close();
				}

				return true;
			}
		};

	}

	@Override
	protected void SkinIsChanged()
	{
		// TODO Auto-generated method stub

	}

}
