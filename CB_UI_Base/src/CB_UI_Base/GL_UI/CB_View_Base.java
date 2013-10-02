package CB_UI_Base.GL_UI;

import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;

import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.GL_UI.interfaces.ViewOptionsMenu;
import CB_UI_Base.Math.CB_RectF;
import CB_UI_Base.Math.SizeF;
import CB_Utils.Util.MoveableList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class CB_View_Base extends GL_View_Base implements ViewOptionsMenu
{

	private boolean isDisposed = false;

	// # Constructors
	public CB_View_Base(String Name)
	{
		super(Name);
	}

	public CB_View_Base(float X, float Y, float Width, float Height, String Name)
	{
		super(X, Y, Width, Height, Name);
	}

	public CB_View_Base(float X, float Y, float Width, float Height, GL_View_Base Parent, String Name)
	{
		super(X, Y, Width, Height, Parent, Name);
	}

	public CB_View_Base(CB_RectF rec, String Name)
	{
		super(rec, Name);
	}

	public CB_View_Base(CB_RectF rec, GL_View_Base Parent, String Name)
	{
		super(rec, Parent, Name);
	}

	public CB_View_Base(SizeF size, String Name)
	{
		super(size, Name);
	}

	@Override
	public void onFree()
	{

	}

	public boolean isDisposed()
	{
		return this.isDisposed;
	}

	protected boolean isInitial = false;

	public void resetInitial()
	{
		isInitial = false;
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{

			@Override
			public void run()
			{
				GL.that.renderOnce("ResetInitial");
			}
		}, 50);
	}

	@Override
	protected void render(SpriteBatch batch)
	{

		if (!isInitial)
		{
			isInitial = true;
			Initial();
		}
	}

	/**
	 * Da die meisten Sprite Initialisierungen von Sprites im Render Thread durchgeführt werden müssen, wird diese Methode, zu
	 * Initialisierung im ersten Render Durchgang ausgeführt.
	 */
	protected abstract void Initial();

	@Override
	public void onResized(CB_RectF rec)
	{

	}

	@Override
	public void onParentRezised(CB_RectF rec)
	{

	}

	@Override
	public boolean onLongClick(int x, int y, int pointer, int button)
	{

		return false;
	}

	@Override
	public boolean onTouchDown(int x, int y, int pointer, int button)
	{

		return false;
	}

	@Override
	public boolean onTouchDragged(int x, int y, int pointer, boolean KineticPan)
	{

		return false;
	}

	@Override
	public boolean onTouchUp(int x, int y, int pointer, int button)
	{

		return false;
	}

	@Override
	public void dispose()
	{
		// Remove from RenderViews if registered
		GL.that.removeRenderView(this);

		if (childs == null)
		{
			// set this to null!
			setToNull(this);
		}
		else
		{

			try
			{
				synchronized (childs)
				{
					for (GL_View_Base v : childs)
					{
						removeChild(v);
						v.dispose();
					}

					childs.clear();
					// set this to null!
					setToNull(this);
				}
			}
			catch (ConcurrentModificationException e)
			{
				setToNull(this);
			}
			catch (NullPointerException e)
			{
				setToNull(this);
			}
		}
		isDisposed = true;
	}

	public static void setToNull(CB_View_Base view)
	{
		if (view.childs == null)
		{
			view = null;
		}
		else
		{
			synchronized (view.childs)
			{
				view.childs = null;
				view = null;
			}
		}
	}

	public int getCildCount()
	{
		if (childs == null) return -1;
		synchronized (childs)
		{
			return childs.size();
		}
	}

	public GL_View_Base addChildDirekt(final GL_View_Base view)
	{
		if (childs == null || view == null) return null;
		synchronized (childs)
		{
			if (!childs.contains(view)) childs.add(view);
		}

		return view;
	}

	public GL_View_Base addChildDirektLast(final GL_View_Base view)
	{
		if (childs == null || view == null) return null;
		synchronized (childs)
		{
			if (!childs.contains(view)) childs.add(0, view);
		}

		return view;
	}

	public void removeChildsDirekt()
	{
		if (childs == null) return;
		synchronized (childs)
		{
			childs.clear();
		}
	}

	public void removeChildsDirekt(GL_View_Base view)
	{
		if (childs == null || view == null) return;
		synchronized (childs)
		{
			try
			{
				if (childs.contains(view)) childs.remove(view);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void removeChildsDirekt(MoveableList<GL_View_Base> childs)
	{
		if (childs == null) return;
		synchronized (childs)
		{
			try
			{
				childs.remove(childs);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public GL_View_Base getChild(int i)
	{
		if (childs == null) return null;
		synchronized (childs)
		{
			if (childs.size() < i || childs.size() == 0) return null;
			return childs.get(i);
		}
	}

	@Override
	public String toString()
	{
		return getName() + " X,Y/Width,Height = " + this.Pos.x + "," + this.Pos.y + "/" + this.width + "," + this.height;
	}

	// Designing this ( a page, a box, a panel, ...) by adding rows of objects<GL_View_Base>
	// the position and width (stretched equally, weighted or percentual on this) of the objects is calculated automatically
	private MoveableList<GL_View_Base> row;
	private boolean topdown = true; // false = bottomup
	private float rowYPos = 0;
	private float rowMaxHeight = 0;
	private float xMargin = 0;
	private float yMargin = 0;
	private float topYAdd;
	private float bottomYAdd = -1;
	public static final int FIXED = -1;
	public static final boolean TOPDOWN = true;
	public static final boolean BOTTOMUP = false;

	/**
	 ** used Height from Bottom with correct topborder
	 **/
	public float getHeightFromBottom()
	{
		return rowYPos - yMargin + topBorder;
	}

	/**
	 ** next objects are added at this Y - Position
	 **/
	public float getRowYPos()
	{
		return rowYPos;
	}

	/**
	 ** next objects are added at this Y - Position
	 **/
	public void setRowYPos(float YPos)
	{
		rowYPos = YPos;
	}

	/**
	 ** setting the margins between the added objects
	 **/
	public void setMargins(float xMargin, float yMargin)
	{
		this.xMargin = xMargin;
		this.yMargin = yMargin;
	}

	/**
	 ** start objects at top
	 **/
	public void initRow()
	{
		initRow(TOPDOWN);
	}

	/**
	 ** start objects at top (direction true) or bottom (direction false)
	 **/
	public void initRow(boolean direction)
	{
		if (direction)
		{
			initRow(direction, this.height - this.topBorder);
		}
		else
		{
			// starting at 0
			initRow(direction, this.bottomBorder);
		}
	}

	/**
	 ** start objects at this y Position, direction true = topdown
	 **/
	public void initRow(boolean direction, float y)
	{
		if (this.row == null)
		{
			this.row = new MoveableList<GL_View_Base>();
		}
		else
		{
			this.row.clear();
		}
		this.rowYPos = y;
		if (bottomYAdd < 0)
		{
			// nur beim ersten Mal, sonst müssen die Werte erhalten bleiben
			if (direction)
			{
				this.bottomYAdd = this.bottomBorder;
				this.topYAdd = y;
			}
			else
			{
				this.bottomYAdd = y;
				this.topYAdd = this.height - this.topBorder;
			}
		}
		this.topdown = direction;
	}

	/**
	 ** get innerHeight - Height of all placed objects
	 **/
	public float getAvailableHeight()
	{
		if (this.row == null) this.initRow();
		return this.topYAdd - this.bottomYAdd;
	}

	public void adjustHeight()
	{
		// nicht sinnvoll wenn von unten und von oben was hinzugefügt wurde
		// und danach auch bitte nichts mehr hinzufügen.
		if (this.topdown)
		{
			this.setHeight(this.getHeight() - this.topYAdd);
			// Die Position aller Clients muss bei TopDown neu gesetzt werden.
			for (GL_View_Base g : this.childs)
			{
				g.setPos(g.getPos().x, g.getPos().y - this.topYAdd);
			}
			// this.topYAdd = this.bottomYAdd; // fertig gebaut
		}
		else
		{
			this.setHeight(this.bottomYAdd);
			// this.topYAdd = this.bottomYAdd; // fertig gebaut
		}
	}

	// Note: Final Position and Size of objects is done on addLast
	// Note: Changing of objects (depending on final Position or Size) must be done after addLast
	// Examples: setting Text of a Button, ....
	// Now, after rework of Label, this is no longer true. Text will be set correctly (independent of call order)
	/**
	 ** Add the object at the end of the current row. the current row will be ended after the object is added.
	 **/
	public void addLast(GL_View_Base c)
	{
		c.Weight = 1f;
		addMe(c, true);
	}

	/**
	 * Add the object at the end of the current row.
	 **/
	public void addNext(GL_View_Base c)
	{
		c.Weight = 1f;
		addMe(c, false);
	}

	/**
	 ** Add the object at the end of the current row. the current row will be ended after the object is added.
	 **/
	public void addLast(GL_View_Base c, float Weight)
	{
		c.Weight = Weight;
		addMe(c, true);
	}

	/**
	 * Add the object at the end of the current row.
	 **/
	public void addNext(GL_View_Base c, float Weight)
	{
		c.Weight = Weight;
		addMe(c, false);
	}

	/**
	 * All items within the last row processed in the layout!
	 */
	public void FinaliseRow()
	{
		GL_View_Base lastItem = this.row.remove(this.row.size() - 1);
		addLast(lastItem);
	}

	/**
	 * All items within the last row processed in the layout!
	 */
	public void FinaliseRow(float Weight)
	{
		GL_View_Base lastItem = this.row.remove(this.row.size() - 1);
		addLast(lastItem, Weight);
	}

	/**
	 * Returns True, if all added Line Child's are layouted
	 * 
	 * @return
	 */
	public boolean RowIsFinalise()
	{
		return this.row.size() == 0;
	}

	// ===================================================================
	private void addMe(GL_View_Base c, boolean lastInRow)
	// ===================================================================
	{
		if (this.row == null) this.initRow();
		if (c != null) this.row.add(c);
		if (lastInRow)
		{
			// Determine this.rowMaxHeight
			this.rowMaxHeight = 0;
			for (GL_View_Base g : this.row)
			{
				if (g.getHeight() > this.rowMaxHeight) this.rowMaxHeight = g.getHeight();
			}
			if (this.topdown)
			{
				this.rowYPos = this.rowYPos - this.rowMaxHeight;
			}
			// Determine width of objects from number of objects in row
			float rowXPos = this.leftBorder;
			float weightedAnz = 0;
			float fixedWidthSum = 0;
			float percentWidthSum = 0;
			float widthToFill = this.width - this.leftBorder - this.rightBorder;
			for (GL_View_Base g : this.row)
			{
				if (g.Weight > 0)
				{
					weightedAnz += g.Weight;
				}
				else
				{
					if (g.Weight == FIXED)
					{
						fixedWidthSum += g.getWidth() + this.xMargin; // xMargin is added to each object
					}
					else
					{
						// Prozentuale Breite des Objekts bzgl widthToFill
						percentWidthSum += Math.abs(g.Weight) * widthToFill;
					}
				}

			}
			float objectWidth = 0;
			if (weightedAnz != 0)
			{
				objectWidth = (widthToFill - percentWidthSum - fixedWidthSum + this.xMargin) / weightedAnz - this.xMargin;
			}
			for (GL_View_Base g : this.row)
			{
				if (g.Weight > 0)
				{
					g.setWidth(objectWidth * g.Weight);
				}
				else
				{
					if (g.Weight > FIXED)
					{
						g.setWidth(Math.abs(g.Weight) * widthToFill - this.xMargin);
					}
				}
				g.setPos(rowXPos, this.rowYPos);
				this.addChildDirekt(g);
				// next object at x
				rowXPos = rowXPos + g.getWidth() + this.xMargin;
			}
			// next row objects at y
			if (this.topdown)
			{
				this.rowYPos = this.rowYPos - this.yMargin;
				this.topYAdd = this.rowYPos;
			}
			else
			{
				this.rowYPos = this.rowYPos + this.rowMaxHeight + this.yMargin;
				this.bottomYAdd = this.rowYPos;
				// todo vielleicht automatische Höhenanpassung von this
			}
			this.row.clear();
		}
	}

}
