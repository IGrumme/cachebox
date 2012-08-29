package CB_Core.GL_UI.Main.Actions;

import CB_Core.GlobalCore;
import CB_Core.Events.platformConector;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.Controls.MessageBox.GL_MsgBox;
import CB_Core.GL_UI.Controls.MessageBox.GL_MsgBox.OnMsgBoxClickListener;
import CB_Core.GL_UI.Controls.MessageBox.MessageBoxButtons;
import CB_Core.GL_UI.Controls.MessageBox.MessageBoxIcon;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class CB_Action_ShowQuit extends CB_Action
{

	public CB_Action_ShowQuit()
	{
		super("quit", AID_SHOW_QUIT);
	}

	@Override
	public void Execute()
	{
		GL_MsgBox.Show(GlobalCore.Translations.Get("QuitReally"), GlobalCore.Translations.Get("Quit?"), MessageBoxButtons.OKCancel,
				MessageBoxIcon.Stop, new OnMsgBoxClickListener()
				{

					@Override
					public boolean onClick(int which)
					{
						if (which == GL_MsgBox.BUTTON_POSITIVE)
						{
							platformConector.callQuitt();
						}
						return true;
					}
				});
	}

	@Override
	public boolean getEnabled()
	{
		return true;
	}

	@Override
	public Sprite getIcon()
	{
		return SpriteCache.Icons.get(31);
	}
}
