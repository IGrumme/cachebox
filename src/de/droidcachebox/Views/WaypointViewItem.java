package de.droidcachebox.Views;

import de.droidcachebox.Config;
import de.droidcachebox.Global;
import de.droidcachebox.R;
import de.droidcachebox.UnitFormatter;
import de.droidcachebox.Components.ActivityUtils;
import de.droidcachebox.Geocaching.Cache;
import de.droidcachebox.Geocaching.Coordinate;
import de.droidcachebox.Geocaching.Waypoint;
import de.droidcachebox.Geocaching.Cache.DrawStyle;
import android.R.bool;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;

public class WaypointViewItem extends View {
    private Cache cache;
    private Waypoint waypoint;
    private int mAscent;
    private int width;
    private int height;
    private boolean BackColorChanger = false;
    private final int CornerSize =20;
    private int rightBorder;
    private int imgSize;

	public WaypointViewItem(Context context, Cache cache, Waypoint waypoint, Boolean BackColorId) {
		// TODO Auto-generated constructor stub
		super(context);
        this.cache = cache;
        this.waypoint = waypoint;
        
        BackColorChanger = BackColorId;
       }

	
	private StaticLayout LayoutName; 
	private StaticLayout LayoutDesc;
	private StaticLayout LayoutCord;
	private StaticLayout LayoutClue;
	private TextPaint LayoutTextPaint;
	private TextPaint LayoutTextPaintBold;
	private int LineSep;
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		measureWidth(widthMeasureSpec);
		this.imgSize = (int) ((WaypointView.windowH / 5) * 0.6);
		this.rightBorder =(int)(WaypointView.windowH / 5);
		int TextWidth = this.width- this.imgSize - this.rightBorder; 
		
		Rect bounds = new Rect();
		LayoutTextPaint = new TextPaint();
		LayoutTextPaint.setTextSize(Global.scaledFontSize_normal);
		LayoutTextPaint.getTextBounds("T", 0, 1, bounds);
		LineSep = bounds.height()/3;
		
		if (waypoint == null) // this Item is the Cache
        {
			this.height = bounds.height()*7;
        }
		else
		{
			String Clue = "";
			if (waypoint.Clue != null)
				Clue = waypoint.Clue;
			LayoutTextPaint.setAntiAlias(true);
			LayoutTextPaint.setColor(Global.getColor(R.attr.TextColor));
			LayoutCord = new StaticLayout(Global.FormatLatitudeDM(waypoint.Latitude()) + " / " + Global.FormatLongitudeDM(waypoint.Longitude()), LayoutTextPaint, TextWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
			LayoutDesc = new StaticLayout(waypoint.Description, LayoutTextPaint, TextWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
			LayoutClue = new StaticLayout(Clue, LayoutTextPaint, TextWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
			LayoutTextPaintBold = new TextPaint(LayoutTextPaint);
			LayoutTextPaintBold.setFakeBoldText(true);
			LayoutName = new StaticLayout(waypoint.GcCode + ": " + waypoint.Title, LayoutTextPaintBold, TextWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
			this.height = (LineSep*5)+ LayoutCord.getHeight()+LayoutDesc.getHeight()+LayoutClue.getHeight()+LayoutName.getHeight();
		}
        
        setMeasuredDimension(this.width,this.height);
		
	}
    
    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = (int) Global.Paints.mesurePaint.measureText(cache.Name) + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        width = specSize;
        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) Global.Paints.mesurePaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = (int) (-mAscent + Global.Paints.mesurePaint.descent()) + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
       
        Boolean isSelected = false;
        if (Global.SelectedWaypoint() == waypoint ||(( Global.SelectedCache()== cache && !(waypoint == null)&& Global.SelectedWaypoint() == waypoint )))
        {
        	isSelected=true;
        }
       
        canvas.drawColor(Global.getColor(R.attr.myBackground));
        int BackgroundColor;
        if (BackColorChanger)
        {
        	BackgroundColor = (isSelected)? Global.getColor(R.attr.ListBackground_select): Global.getColor(R.attr.ListBackground);
        }
        else
        {
        	BackgroundColor = (isSelected)? Global.getColor(R.attr.ListBackground_select): Global.getColor(R.attr.ListBackground_secend);
        }
        
        int LineColor = Global.getColor(R.attr.ListSeparator);
        Rect DrawingRec = new Rect(5, 5, width-5, height-5);
        ActivityUtils.drawFillRoundRecWithBorder(canvas, DrawingRec, 2, LineColor, BackgroundColor);
        
        if (waypoint == null) // this Item is the Cache
        {
             cache.DrawInfo(canvas, DrawingRec, BackgroundColor, Cache.DrawStyle.withoutSeparator);    
        }
        else
        {	
        	
        	int left= 15;
        	int top = LineSep *2;
        	
        	int iconWidth = 0;
        	// draw icon
        	if (((int)waypoint.Type.ordinal()) < Global.CacheIconsBig.length)
        		iconWidth=ActivityUtils.PutImageTargetHeight(canvas, Global.CacheIconsBig[(int)waypoint.Type.ordinal()], CornerSize/2,CornerSize, imgSize);

        	// draw Text info
        	left += iconWidth;
        	top += ActivityUtils.drawStaticLayout(canvas, LayoutName, left, top);
        	top += ActivityUtils.drawStaticLayout(canvas, LayoutDesc, left, top);
        	top += ActivityUtils.drawStaticLayout(canvas, LayoutCord, left, top);
        	if (waypoint.Clue != null)ActivityUtils.drawStaticLayout(canvas, LayoutClue, left, top);
        	
        	// draw Arrow and distance
        	// Draw Bearing
        	if (Cache.BearingRec!=null)
        		cache.DrawBearing(canvas,Cache.BearingRec,waypoint);
        	
         
        }
    }
}
