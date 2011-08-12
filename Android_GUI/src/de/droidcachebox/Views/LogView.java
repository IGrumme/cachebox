package de.droidcachebox.Views;

import java.util.ArrayList;

import CB_Core.Config;
import de.droidcachebox.Database;
import de.droidcachebox.Global;
import de.droidcachebox.R;
import CB_Core.Events.SelectedCacheEvent;
import CB_Core.Events.SelectedCacheEventList;
import de.droidcachebox.Events.ViewOptionsMenu;
import de.droidcachebox.Ui.ActivityUtils;
import CB_Core.Types.Cache;
import CB_Core.Types.LogEntry;
import CB_Core.Types.Waypoint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class LogView extends ListView implements SelectedCacheEvent, ViewOptionsMenu{

	Cache aktCache;
	boolean mustLoad;
	CustomAdapter lvAdapter;
	/**
	 * Constructor
	 */
	private String text;
	public LogView(Context context) {
		super(context);
		mustLoad = false;

		SelectedCacheEventList.Add(this);
		this.setAdapter(null);

		this.setLongClickable(true);
		this.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
/*				Waypoint aktWaypoint = null;
				if (arg2 > 0)
					aktWaypoint = Global.SelectedCache().waypoints.get(arg2 - 1);
        		Global.SelectedWaypoint(Global.SelectedCache(), aktWaypoint);*/
				return true;
			}
		});
		ActivityUtils.setListViewPropertys(this);
		
	}

	public class CustomAdapter extends BaseAdapter /*implements OnClickListener*/ {
		 
		/*private class OnItemClickListener implements OnClickListener{
		    private int mPosition;
		    OnItemClickListener(int position){
		            mPosition = position;
		    }
		    public void onClick(View arg0) {
		            Log.v("ddd", "onItemClick at position" + mPosition);
		    }
		}*/
	 
	    private Context context;
	    private Cache cache;
	    private ArrayList<LogEntry> logs;
	 
	    public CustomAdapter(Context context, Cache cache ) {
	    	
	        this.context = context;
	        this.cache = cache;
	        ArrayList<LogEntry> cleanLogs = new ArrayList<LogEntry>();
	        cleanLogs = Database.Logs(cache);// cache.Logs();
	        
	        //clean up logs
	        logs = new ArrayList<LogEntry>();
	        for(LogEntry l:cleanLogs){if(l.TypeIcon!=-1)logs.add(l);}
	       
	    }
	 
	    public void setCache(Cache cache) {
	    	this.cache = cache;
	    
	    }
	    public int getCount() {
	    	if (cache != null)
	    	{
	    		return logs.size();
	    	}
	    	else
	    	{
	    		return 0;
	    	}
	    }
	 
	    public Object getItem(int position) {
	    	if (cache != null)
	    	{
	    		return logs.get(position);
	    	} else
	    		return null;
	    }
	 
	    public long getItemId(int position) {
	        return position;
	    }
	 
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	if (cache != null)
	    	{
		        LogEntry logEntry = logs.get(position);
		        Boolean BackGroundChanger = ((position % 2) == 1);
		        LogViewItem v = new LogViewItem(context, cache, logEntry,BackGroundChanger);
		        return v;
	    	} else
	    		return null;
	    }
	 
	 
	}

	@Override
	public boolean ItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void SelectedCacheChanged(Cache cache, Waypoint waypoint) {
		if (aktCache != cache)
		{
			aktCache = cache;
			mustLoad = true;
		}
	}

	@Override
	public void BeforeShowMenu(Menu menu) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnShow() {
		// TODO Auto-generated method stub
		if (mustLoad)
		{
			this.setAdapter(null);
			lvAdapter = new CustomAdapter(getContext(), aktCache);
			this.setAdapter(lvAdapter);
			lvAdapter.notifyDataSetChanged();
			mustLoad = false;
		}
	}

	@Override
	public void OnHide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnFree() {
		
	}

	@Override
	public int GetMenuId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void ActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int GetContextMenuId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void BeforeShowContextMenu(Menu menu) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean ContextMenuItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return false;
	}


}
