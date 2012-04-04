package CB_Core.GL_UI.Main.Actions;

import CB_Core.Config;
import CB_Core.GlobalCore;
import CB_Core.DB.Database;
import CB_Core.GL_UI.CB_View_Base;
import CB_Core.GL_UI.GL_View_Base;
import CB_Core.GL_UI.GL_View_Base.OnClickListener;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.GL_UI.Main.TabMainView;
import CB_Core.GL_UI.Menu.Menu;
import CB_Core.GL_UI.Menu.MenuItem;
import CB_Core.GL_UI.Views.CacheListView;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class CB_Action_ShowCacheList extends CB_Action_ShowView
{
	public final int MI_MANAGE_DB = 1;
	public final int MI_AUTO_RESORT = 2;
	public final int MI_RESORT = 3;
	public final int MI_FilterSet = 4;
	public final int MI_SEARCH = 5;
	public final int MI_IMPORT = 6;

	public CB_Action_ShowCacheList()
	{
		super("cacheList", AID_SHOW_CACHELIST);
	}

	@Override
	public void Execute()
	{
		if ((TabMainView.cacheListView == null) && (tabMainView != null) && (tab != null)) TabMainView.cacheListView = new CacheListView(
				tab.getContentRec(), "CacheListView");

		if ((TabMainView.cacheListView != null) && (tab != null)) tab.ShowView(TabMainView.cacheListView);
	}

	@Override
	public CB_View_Base getView()
	{
		return TabMainView.cacheListView;
	}

	@Override
	public boolean getEnabled()
	{
		return true;
	}

	@Override
	public Sprite getIcon()
	{
		return SpriteCache.Icons.get(7);
	}

	@Override
	public boolean HasContextMenu()
	{
		return true;
	}

	@Override
	public boolean ShowContextMenu()
	{
		Menu cm = new Menu("CacheListContextMenu");

		cm.setItemClickListner(new OnClickListener()
		{

			@Override
			public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
			{
				switch (((MenuItem) v).getMenuItemId())
				{
				case MI_RESORT:
					Database.Data.Query.Resort();
					return true;

				}
				return false;
			}
		});

		String DBName = Config.settings.DatabasePath.getValue();
		int Pos = DBName.lastIndexOf("/");
		DBName = DBName.substring(Pos + 1);
		Pos = DBName.lastIndexOf(".");
		DBName = DBName.substring(0, Pos);

		MenuItem mi;

		mi = cm.addItem(MI_MANAGE_DB, "manage", "  (" + DBName + ")");
		mi = cm.addItem(MI_AUTO_RESORT, "AutoResort");
		mi.setCheckable(true);
		mi.setChecked(GlobalCore.autoResort);
		cm.addItem(MI_RESORT, "ResortList");
		cm.addItem(MI_FilterSet, "filter");
		cm.addItem(MI_SEARCH, "search");
		// Global.TranslateMenuItem(IconMenu, R.id.miAddCache, "ManuallyAddCache");
		cm.addItem(MI_IMPORT, "import");

		cm.show();

		return true;
	}
}
