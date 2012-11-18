package CB_Core.GL_UI.Main.Actions;

import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.Controls.PopUps.SearchDialog;
import CB_Core.GL_UI.Main.TabMainView;
import CB_Core.GL_UI.Menu.MenuID;
import CB_Core.GL_UI.Views.CacheListView;
import CB_Core.GL_UI.Views.MapView;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class CB_Action_Show_Search extends CB_Action
{

	public CB_Action_Show_Search()
	{
		super("search", MenuID.AID_SEARCH);
	}

	@Override
	public void Execute()
	{

		if (MapView.that == null || !MapView.that.isVisible())
		{
			if (CacheListView.that == null || !CacheListView.that.isVisible())
			{
				TabMainView.actionShowCacheList.Execute();
			}
		}

		if (SearchDialog.that == null)
		{
			new SearchDialog();
		}

		SearchDialog.that.showNotCloseAutomaticly();
	}

	@Override
	public boolean getEnabled()
	{
		return true;
	}

	@Override
	public Sprite getIcon()
	{
		return SpriteCache.Icons.get(27);
	}
}
