package CB_Core.GL_UI.Controls.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import CB_Core.GL_UI.GL_View_Base;
import CB_Core.GL_UI.GL_Listener.GL_Listener;
import CB_Core.Math.CB_RectF;

public class V_ListView extends ListViewBase
{

	public V_ListView(CB_RectF rec, CharSequence Name)
	{
		super(rec, Name);
	}

	ArrayList<ListViewItemBase> clearList = new ArrayList<ListViewItemBase>();

	protected void RenderThreadSetPos(float value)
	{
		float distance = mPos - value;

		clearList.clear();

		// alle childs verschieben

		if (mReloadItems)
		{
			mAddeedIndexList.clear();
			if (mCanDispose)
			{
				synchronized (childs)
				{
					for (GL_View_Base v : childs)
					{
						v.dispose();
					}
				}
			}
			this.removeChilds();
		}
		else
		{
			synchronized (childs)
			{
				for (GL_View_Base tmp : childs)
				{
					tmp.setY(tmp.getY() + distance);

					if (tmp.getY() > this.getMaxY() || tmp.getMaxY() < 0)
					{
						// Item ist nicht mehr im sichtbaren Bereich!
						clearList.add((ListViewItemBase) tmp);
					}
				}
			}
		}

		mReloadItems = false;

		// afr�umen
		if (clearList.size() > 0)
		{
			for (Iterator<ListViewItemBase> iterator = clearList.iterator(); iterator.hasNext();)
			{
				ListViewItemBase tmp = iterator.next();
				mAddeedIndexList.remove((Object) tmp.getIndex());
				// Logger.LogCat("Remove Item " + tmp.getIndex());
				this.removeChild(tmp);
				if (mCanDispose) tmp.dispose();
			}
			clearList.clear();

			// setze First Index, damit nicht alle Items durchlaufen werden m�ssen
			Collections.sort(mAddeedIndexList);
			if (mAddeedIndexList.size() > 0)
			{
				mFirstIndex = mAddeedIndexList.get(0) - mMaxItemCount;
				if (mFirstIndex < 0) mFirstIndex = 0;
			}
			else
			{
				mFirstIndex = 0;
			}

		}

		mPos = value;

		addVisibleItems();
		mMustSetPos = false;

	}

	protected void addVisibleItems()
	{
		if (mBaseAdapter == null) return;
		if (mPosDefault == null) calcDefaultPosList();

		for (int i = mFirstIndex; i < mBaseAdapter.getCount(); i++)
		{
			if (!mAddeedIndexList.contains(i))
			{

				float itemPos = mPosDefault.get(i);
				itemPos -= mPos;

				if (itemPos < this.getMaxY() && itemPos + mBaseAdapter.getItemSize(i) > 0)
				{
					ListViewItemBase tmp = mBaseAdapter.getView(i);
					tmp.setY(itemPos);
					if (i == mSelectedIndex)
					{
						tmp.isSelected = true;
						tmp.resetInitial();
					}
					this.addChild(tmp);
					// Logger.LogCat("Add Item " + i);
					mAddeedIndexList.add(tmp.getIndex());
				}

				else if (itemPos + mBaseAdapter.getItemSize(i) < 0)
				{
					break;
				}

			}

			// RenderRequest
			GL_Listener.glListener.renderOnce(this);
		}
	}

	protected void calcDefaultPosList()
	{
		if (mPosDefault != null)
		{
			mPosDefault.clear();
			mPosDefault = null;
		}

		mPosDefault = new ArrayList<Float>();

		float minimumItemHeight = this.height;

		float countPos = this.height - mDividerSize;

		for (int i = mFirstIndex; i < mBaseAdapter.getCount(); i++)
		{
			float itemHeight = mBaseAdapter.getItemSize(i);

			countPos -= itemHeight + mDividerSize;
			mPosDefault.add(countPos);

			if (itemHeight < minimumItemHeight) minimumItemHeight = itemHeight;

		}
		mAllSize = countPos;
		mMaxItemCount = (int) (this.height / minimumItemHeight);
		if (mMaxItemCount < 1) mMaxItemCount = 1;
	}

	@Override
	public boolean onTouchDragged(int x, int y, int pointer)
	{
		if (!mIsDrageble) return false;
		mDraged = y - mLastTouch;
		setListPos(mLastPos_onTouch - mDraged);
		return true;
	}

	@Override
	public boolean onTouchDown(int x, int y, int pointer, int button)
	{
		if (!mIsDrageble) return false;
		mLastTouch = y;
		mLastPos_onTouch = mPos;
		return true; // muss behandelt werden, da sonnst kein onTouchDragged() ausgel�st wird.
	}

	@Override
	protected void startAnimationtoTop()
	{
		if (mBaseAdapter == null) return;
		mBottomAnimation = false;
		scrollTo(mBaseAdapter.getItemSize(0));
	}

	@Override
	protected void Initial()
	{
		// TODO Auto-generated method stub

	}

}
