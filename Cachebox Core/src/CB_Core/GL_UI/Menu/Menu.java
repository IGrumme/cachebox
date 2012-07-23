package CB_Core.GL_UI.Menu;

import java.util.ArrayList;
import java.util.Iterator;

import CB_Core.GlobalCore;
import CB_Core.GL_UI.GL_View_Base;
import CB_Core.GL_UI.Controls.Dialog;
import CB_Core.GL_UI.Controls.List.Adapter;
import CB_Core.GL_UI.Controls.List.ListViewItemBase;
import CB_Core.GL_UI.Controls.List.V_ListView;
import CB_Core.GL_UI.GL_Listener.GL_Listener;
import CB_Core.Math.CB_RectF;
import CB_Core.Math.GL_UISizes;
import CB_Core.Math.SizeF;
import CB_Core.Math.UiSizes;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class Menu extends Dialog
{
	public static CB_RectF MENU_REC = new CB_RectF(0, 0, 400, 60); // wird mit jedem Item gr��er
	public float ItemHeight = -1f;

	private ArrayList<MenuItem> mItems = new ArrayList<MenuItem>();
	private V_ListView mListView;
	private Menu that;

	private OnClickListener MenuItemClickListner = new OnClickListener()
	{

		@Override
		public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
		{
			GL_Listener.glListener.closeDialog(that);
			if (mOnItemClickListner != null) mOnItemClickListner.onClick(v, x, y, pointer, button);

			return true;
		}
	};

	public Menu(String Name)
	{
		super(MENU_REC, Name);
		that = this;

		if (ItemHeight == -1f) ItemHeight = UiSizes.getButtonHeight();

		MENU_REC = new CB_RectF(new SizeF(400, mHeaderHight + mFooterHeight + (margin * 2)));

		this.setRec(MENU_REC);

		mListView = new V_ListView(this, "MenuList");

		mListView.setWidth(this.width);
		mListView.setHeight(this.height);

		mListView.setZeroPos();
		this.addChild(mListView);

	}

	@Override
	protected void Initial()
	{
		super.Initial();

		this.removeChilds();

		mListView.setSize(this.getContentSize());

		this.addChild(mListView);
		mListView.setBaseAdapter(new CustomAdapter());

		super.Initial();
		if (mListView.getMaxItemCount() < mItems.size())
		{
			mListView.setDragable();
		}
		else
		{
			mListView.setUndragable();
		}
	}

	public class CustomAdapter implements Adapter
	{

		public ListViewItemBase getView(int position)
		{
			ListViewItemBase v = mItems.get(position);
			v.resetInitial();
			return v;
		}

		@Override
		public int getCount()
		{
			return mItems.size();
		}

		@Override
		public float getItemSize(int position)
		{
			if (mItems == null || mItems.size() == 0 || mItems.size() < position) return 0;
			return mItems.get(position).getHeight();
		}
	}

	public void addItem(MenuItem menuItem)
	{
		menuItem.setOnClickListener(MenuItemClickListner);
		mItems.add(menuItem);
	}

	public MenuItem addItem(int ID, String StringId)
	{
		return addItem(ID, StringId, "", false);
	}

	public MenuItem addItem(int ID, String StringId, boolean withoutTranslation)
	{
		return addItem(ID, StringId, "", withoutTranslation);
	}

	public MenuItem addItem(int ID, String StringId, String anhang, Sprite icon)
	{
		MenuItem item = addItem(ID, StringId, anhang);
		item.setIcon(icon);
		return item;
	}

	public MenuItem addItem(int ID, String StringId, String anhang)
	{
		return addItem(ID, StringId, anhang, false);
	}

	public MenuItem addItem(int ID, String StringId, String anhang, boolean withoutTranslation)
	{
		String trans;
		if (StringId == null || StringId.equals(""))
		{
			trans = anhang;
		}
		else
		{
			trans = GlobalCore.Translations.Get(StringId) + anhang;
		}

		if (withoutTranslation) trans = StringId;

		float higherValue = this.height + ItemHeight + mListView.getDividerHeight();

		if (higherValue < UiSizes.getWindowHeight() * 0.8f)
		{
			this.setSize(GL_UISizes.UI_Left.getWidth() / 1.2f, higherValue);

			this.resetInitial();
		}

		// mListView.setWidth(this.width - Left - Reight);
		// mListView.setHeight(this.height - mFooterHeight - mHeaderHight - Top - Bottom - margin);

		mListView.setSize(this.getContentSize());
		mListView.setHeight(mListView.getHeight());
		MenuItem item = new MenuItem(new SizeF(mListView.getWidth(), ItemHeight), mItems.size(), ID, "Menu Item@" + ID);

		item.setTitle(trans);
		addItem(item);
		mListView.notifyDataSetChanged();
		return item;
	}

	public void show()
	{

		// wenn irgent ein Item Chackable ist, dann alle Titles Einr�cken.
		boolean oneIsChakable = false;
		for (Iterator<MenuItem> iterator = mItems.iterator(); iterator.hasNext();)
		{
			if (iterator.next().isCheckable())
			{
				oneIsChakable = true;
				break;
			}
		}
		if (oneIsChakable)
		{
			for (Iterator<MenuItem> iterator = mItems.iterator(); iterator.hasNext();)
			{
				iterator.next().setLeft(true);
			}
		}

		GL_Listener.glListener.showDialog(this);

	}

	public MenuItem addItem(int ID, String StringId, Sprite icon)
	{
		MenuItem item = addItem(ID, StringId);
		item.setIcon(icon);
		return item;
	}

	private OnClickListener mOnItemClickListner;

	public void setItemClickListner(OnClickListener onItemClickListner)
	{
		this.mOnItemClickListner = onItemClickListner;
	}

	@Override
	protected void SkinIsChanged()
	{
		// TODO Auto-generated method stub

	}

}
