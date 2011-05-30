package de.droidcachebox;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

import org.mapsforge.preprocessing.highwayHierarchies.hierarchyComputation.util.DBConnection;

import de.droidcachebox.ExtAudioRecorder;
import de.droidcachebox.Components.ActivityUtils;
import de.droidcachebox.Components.CacheNameView;
import de.droidcachebox.Custom_Controls.IconContextMenu.IconContextMenu;
import de.droidcachebox.Custom_Controls.IconContextMenu.IconContextMenu.IconContextItemSelectedListener;
import de.droidcachebox.Events.CachListChangedEventList;
import de.droidcachebox.Events.CacheListChangedEvent;
import de.droidcachebox.Events.PositionEventList;
import de.droidcachebox.Events.SelectedCacheEvent;
import de.droidcachebox.Events.SelectedCacheEventList;
import de.droidcachebox.Events.ViewOptionsMenu;
import de.droidcachebox.Locator.Locator;
import de.droidcachebox.Map.Descriptor;
import de.droidcachebox.Views.CacheListView;
import de.droidcachebox.Views.CompassView;
import de.droidcachebox.Views.DescriptionView;
import de.droidcachebox.Views.EmptyViewTemplate;
import de.droidcachebox.Views.FieldNotesView;
import de.droidcachebox.Views.LogView;
import de.droidcachebox.Views.MapView;
import de.droidcachebox.Views.NotesView;
import de.droidcachebox.Views.SolverView;
import de.droidcachebox.Views.SpoilerView;
import de.droidcachebox.Views.WaypointView;
import de.droidcachebox.Views.FilterSettings.EditFilterSettings;
import de.droidcachebox.Views.FilterSettings.PresetListView;
import de.droidcachebox.Views.Forms.EditWaypoint;
import de.droidcachebox.Views.Forms.HintDialog;
import de.droidcachebox.Views.Forms.ScreenLock;
import de.droidcachebox.Views.Forms.Settings;
import de.droidcachebox.Database;
import de.droidcachebox.Database.DatabaseType;
import de.droidcachebox.Geocaching.Cache;
import de.droidcachebox.Geocaching.Coordinate;
import de.droidcachebox.Geocaching.Waypoint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class main extends Activity implements SelectedCacheEvent,LocationListener,CacheListChangedEvent {
    LayoutInflater inflater;
	private ImageButton buttonDB;
	private ImageButton buttonCache;
	private ImageButton buttonMap;
	private ImageButton buttonInfo;
	private ImageButton buttonMisc;
	private FrameLayout frame;
	private RelativeLayout TopLayout;
	private FrameLayout frameCacheName;
	public static Activity mainActivity;
	
// Views
	private static Integer aktViewId = -1;
	private ViewOptionsMenu aktView = null;
	private CacheNameView cacheNameView;
	
	private MapView mapView;					// ID 0
	private CacheListView cacheListView;		// ID 1
	private WaypointView waypointView;			// ID 2
	private LogView logView;					// ID 3
	private DescriptionView descriptionView;	// ID 4
	private SpoilerView spoilerView;			// ID 5
	private NotesView notesView;				// ID 6
	private SolverView solverView;				// ID 7
	private CompassView compassView;			// ID 8
	private FieldNotesView fieldNotesView;		// ID 9
	private EmptyViewTemplate TestEmpty;		// ID 10
	private ArrayList<View> ViewList = new ArrayList<View>();
	
	int width;
    int height;

	
	// Powermanager
    protected PowerManager.WakeLock mWakeLock;
    // GPS
	private LocationManager locationManager;
	private String provider;
	// Compass
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float[] mCompassValues;
    
	// to store, which menu should be viewd
	private enum nextMenuType { nmDB, nmCache, nmMap, nmInfo, nmMisc }
	private nextMenuType nextMenu = nextMenuType.nmDB;

	// Media
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 61216516;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 61216517;
    private static Uri cameraVideoURI;
    private static File mediafile = null;
    private static String mediafilename = null;
    private static String mediaTimeString = null; 
    private static String basename = null;
    private static String mediaCacheName = null;
    private static Coordinate mediaCoordinate = null;
    private static Boolean VoiceRecIsStart = false;
    private ExtAudioRecorder extAudioRecorder = null;

    
    final SensorEventListener mListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
        	mCompassValues = event.values;
        	Global.Locator.setCompassHeading(mCompassValues[0]); 
        	PositionEventList.Call(mCompassValues[0]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    
	
    @Override
	protected void onActivityResult(int requestCode, int resultCode,
		Intent data) {

    	// Intent Result Take Photo
    	if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK){
                Log.d("DroidCachebox","Picture taken!!!");
                Global.selectedCache.ReloadSpoilerRessources();
                String MediaFolder = Config.GetString("UserImageFolder");
            	String TrackFolder = Config.GetString("TrackFolder");
            	String relativPath = Global.getRelativePath(MediaFolder, TrackFolder, "/"); 
            	// Da ein Foto eine Momentaufnahme ist, kann hier die Zeit und die Koordinaten nach der Aufnahme verwendet werden.
            	mediaTimeString = Global.GetTrackDateTimeString();
            	TrackRecorder.AnnotateMedia(basename + ".jpg", relativPath + "/" + basename + ".jpg", Global.LastValidPosition, mediaTimeString);
            	
            	return;
            } else
            {
                Log.d("DroidCachebox","Picture NOT taken!!!");
                return;
            }
        }

    	// Intent Result Record Video
    	if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK){
                Log.d("DroidCachebox","Video taken!!!");
                //Global.selectedCache.ReloadSpoilerRessources();

                String[] projection = { MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE  };
                Cursor cursor = managedQuery(cameraVideoURI, projection, null, null, null);
                int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                String recordedVideoFilePath = cursor.getString(column_index_data);
                
                String ext = Global.GetFileExtension(recordedVideoFilePath);
                String MediaFolder = Config.GetString("UserImageFolder");

                // Video in Media-Ordner verschieben
                File  source = new File(recordedVideoFilePath);
                File destination = new File(MediaFolder + "/" + basename + "." + ext);
                // Datei wird umbenannt/verschoben
                if(!source.renameTo(destination))
                {
                	Log.d("DroidCachebox","Fehler beim Umbenennen der Datei: " + source.getName());
                }
                
            	String TrackFolder = Config.GetString("TrackFolder");
            	String relativPath = Global.getRelativePath(MediaFolder, TrackFolder, "/"); 
            	TrackRecorder.AnnotateMedia(basename + "." + ext, relativPath + "/" + basename + "." + ext, mediaCoordinate , mediaTimeString);
            	
            	return;
            } else
            {
                Log.d("DroidCachebox","Video NOT taken!!!");
                return;
            }
        }

    	if (requestCode == 12345)
    	{
    		counterStopped = false;
    		counter.start();
    		return;
    	}
    	aktView.ActivityResult(requestCode, resultCode, data);
    }


    private class MyCount extends CountDownTimer {
    	public MyCount(long millisInFuture, long countDownInterval) {
    		 super(millisInFuture, countDownInterval);
    		 }        	
    	@Override
    	public void onFinish() {
    		startScreenLock();
//    		Toast.makeText(getApplicationContext(), "timer", Toast.LENGTH_LONG).show();
    	}
		@Override
		public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			
		}        
    }
    MyCount counter = null;
    private boolean counterStopped = false;
    public void startScreenLock()
    {
		counter.cancel();
		counterStopped = true;
		final Intent mainIntent = new Intent().setClass( this, ScreenLock.class);
		this.startActivityForResult(mainIntent, 12345);
    }
    
    @Override
    public void onUserInteraction(){
    	if (counterStopped)
    		return;
    	if (counter != null)
    		counter.start();

    }
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	ActivityUtils.onActivityCreateSetTheme(this);    	
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);
        
        mainActivity= this;
        
        /*
        final Handler handler = new Handler();        
        aTimer = new Timer("ScreenLockTimer");
        TimerTask blinkTimerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                       
                       public void run() {   
                    	   Toast.makeText(getApplicationContext(), "timer", Toast.LENGTH_SHORT).show();
                       }
                });
            }
        };        
        aTimer.schedule(blinkTimerTask, 30000);
*/
        counter = new MyCount(60000, 60000);
        counter.start();

        
        // add Event Handler
        SelectedCacheEventList.Add(this);
        CachListChangedEventList.Add(this);
        
        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
         this.width = d.getWidth();
         this.height = d.getHeight();


        inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);       
        
        frameCacheName = (FrameLayout)this.findViewById(R.id.frameCacheName);
        cacheNameView = new CacheNameView(this);
        frameCacheName.addView(cacheNameView);
        
        TopLayout=(RelativeLayout)this.findViewById(R.id.layoutTop);     
        
        
        frame = (FrameLayout)this.findViewById(R.id.layoutContent);
     
        // Ausschalten verhindern
        /* This code together with the one in onDestroy() 
         * will make the screen be always on until this Activity gets destroyed. */
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        
        // GPS
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the locatioin provider -> use
		// default
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		provider = LocationManager.GPS_PROVIDER;
		Location location = locationManager.getLastKnownLocation(provider);
		Global.Locator = new Locator();
		locationManager.requestLocationUpdates(provider, 1000, 1, this);                
        
        mapView = new MapView(this, inflater);
        mapView.Initialize();
        mapView.CurrentLayer = MapView.Manager.GetLayerByName(Config.GetString("CurrentMapLayer"), Config.GetString("CurrentMapLayer"), "");
        Global.TrackDistance = Config.GetInt("TrackDistance");

        compassView = new CompassView(this, inflater);
        cacheListView = new CacheListView(this);
        waypointView = new WaypointView(this, this);
        logView = new LogView(this);
        fieldNotesView = new FieldNotesView(this, this);
        registerForContextMenu(fieldNotesView);
        descriptionView = new DescriptionView(this, "Cache-Beschreibung");
        spoilerView = new SpoilerView(this, inflater);
        notesView = new NotesView(this, inflater);
        solverView = new SolverView(this, inflater);
        TestEmpty = new EmptyViewTemplate(this, inflater);
        
        this.buttonDB = (ImageButton)this.findViewById(R.id.buttonDB);
        registerForContextMenu(buttonDB);
        this.buttonDB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	showView(cacheListView);
            }
          });
//      this.buttonDB.setLongClickable(true);
  /*      
        this.buttonDB.setOnLongClickListener(new View.OnLongClickListener() {	
			@Override
			public boolean onLongClick(View v) {
            	nextMenu = main.nextMenuType.nmDB;
            	openOptionsMenu();
				return true;
			}
		});
		
		
		*/
        this.buttonCache = (ImageButton)this.findViewById(R.id.buttonCache);
        registerForContextMenu(buttonCache);
        this.buttonCache.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	showView(descriptionView);
            }
          });
        
        this.buttonMap = (ImageButton)this.findViewById(R.id.buttonMap);
        registerForContextMenu(buttonMap);
        this.buttonMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	showView(mapView);
            }
          });
        
        
        this.buttonInfo = (ImageButton)this.findViewById(R.id.buttonInfo);
        registerForContextMenu(buttonInfo);
        this.buttonInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	showView(logView);
            }
          });
        
        this.buttonMisc = (ImageButton)this.findViewById(R.id.buttonMisc);
        registerForContextMenu(buttonMisc);
        this.buttonMisc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	nextMenu = main.nextMenuType.nmMisc;
            	openOptionsMenu();            	
            }
          });
        
        mapView.InitializeMap();
        Global.SelectedCache(Database.Data.Query.get(0));
        
        
        fillViewList();
        if (aktViewId != -1)
        {
        	// Zeige letzte gespeicherte View beim neustart der Activity
        	showView(aktViewId);
        	
        	// Initialisiere Icons neu.
        	 Global.InitIcons(this, Config.GetBool("nightMode"));
        	
        }
    }
    
    void fillViewList()
    {
    	ViewList.add(mapView);				// ID 0
    	ViewList.add(cacheListView);		// ID 1
    	ViewList.add(waypointView);			// ID 2
    	ViewList.add(logView);				// ID 3
    	ViewList.add(descriptionView);		// ID 4
    	ViewList.add(spoilerView);			// ID 5
    	ViewList.add(notesView);			// ID 6
    	ViewList.add(solverView);			// ID 7	
    	ViewList.add(compassView);			// ID 8	
    	ViewList.add(fieldNotesView);		// ID 9
    	ViewList.add(TestEmpty);			// ID 10
    }

    @Override
    public void onDestroy() {
    		if (isFinishing())
    		{
                this.mWakeLock.release();
        		counter.cancel();
        		TrackRecorder.StopRecording();
        		// GPS Verbindung beenden
        		locationManager.removeUpdates(this);
        		// Voice Recorder stoppen
                if (extAudioRecorder != null) 
                {            
		            extAudioRecorder.stop();
		            extAudioRecorder.release();
		            extAudioRecorder = null;
                }
    		}
			super.onDestroy();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        counter.start();
        mSensorManager.registerListener(mListener, mSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop()
    {
        mSensorManager.unregisterListener(mListener);
		counter.cancel();
        super.onStop();
    }

    /** hook into menu button for activity */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
  		return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
    	int menuId = aktView.GetMenuId();
    	if (menuId > 0)
    	{
    		getMenuInflater().inflate(menuId, menu);
    	}
    	aktView.BeforeShowMenu(menu);
      return super.onPrepareOptionsMenu(menu);
    }

    /** when menu button option selected */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	if (aktView != null)
    		return aktView.ItemSelected(item);
    	return super.onOptionsItemSelected(item);
    } 
    
  
    public void showView(Integer viewId)
    {
    	showView((ViewOptionsMenu)ViewList.get(viewId));
    }
    
    private void showView(ViewOptionsMenu view)
    {
    	if (aktView != null)
    		aktView.OnHide();
    	aktView = view;
    	frame.removeAllViews();
    	frame.addView((View) aktView);
    	aktView.OnShow();  
    	aktViewId=ViewList.indexOf(aktView);
    	
    }
    
    
    
    private IconContextMenu icm;
    
    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      
      if (v instanceof ViewOptionsMenu)
      {
    	  int id = ((ViewOptionsMenu)v).GetContextMenuId();
    	  if (id > 0)
    	  {
        	  inflater.inflate(id, menu);
        	  ((ViewOptionsMenu)v).BeforeShowContextMenu(menu);
    	  }
    	  return;
      }
      
      
      
      if (v == buttonDB)
      {
    	  icm = new IconContextMenu(this, R.menu.menu_db);
		icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() 
		{
			
			@Override
			public void onIconContextItemSelected(MenuItem item, Object info) 
			{
				switch (item.getItemId())
		    	{
				// DB
		    	case R.id.miCacheList:
		    		showView(cacheListView);
		    		break;
		    	
		    	case R.id.miFilterset:
		    		final Intent mainIntent = new Intent().setClass( mainActivity, EditFilterSettings.class);
		    		mainActivity.startActivity(mainIntent);
		    		break;
		    	case R.id.miManageDB:
		    		break;
		    	case R.id.miResort:
		    		Database.Data.Query.Resort();
		    		break;
		    	case R.id.miAutoResort:
		    		Global.autoResort = !(Global.autoResort);
		            
		            Config.Set("AutoResort", Global.autoResort);

		            if (Global.autoResort)
		            	Database.Data.Query.Resort();
		    		break;
		    	
		    	}
		    }
		});
		Menu IconMenu=icm.getMenu();
		Global.Translations.TranslateMenuItem(IconMenu, R.id.miCacheList, "cacheList","  (" + String.valueOf(Database.Data.Query.size()) + ")" );
		Global.Translations.TranslateMenuItem(IconMenu, R.id.miFilterset, "filter");
		Global.Translations.TranslateMenuItem(IconMenu, R.id.miManageDB, "manage");
		Global.Translations.TranslateMenuItem(IconMenu, R.id.miResort, "ResortList");
		MenuItem miAutoResort = Global.Translations.TranslateMenuItem(IconMenu, R.id.miAutoResort, "AutoResort");
		miAutoResort.setCheckable(true);
		miAutoResort.setChecked(Global.autoResort);
		//AutoResortButton.ButtonImage = (Global.autoResort) ? Global.Icons[6] : Global.Icons[7];
  	  icm.show();
      }
  	  else if (v == buttonCache)
      {
   		  icm = new IconContextMenu(this, R.menu.menu_cache);
  		  icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() 
  		  {

  			@Override
  			public void onIconContextItemSelected(MenuItem item, Object info) 
  			{
  				switch (item.getItemId())
  		    	{
  			// Cache
  		    	case R.id.miDescription:
  		    		showView(descriptionView);
  		    		break;
  		    	case R.id.miWaypoints:
  		    		showView(waypointView);
  		    		break;
  		    	case R.id.miNotes:
  		    		showView(notesView);
  		    		break;
  		    	case R.id.miSolver:
  		    		showView(solverView);
  		    		break;
  					
  		    	}
  		    }
  		});
    	  
    	  Menu IconMenu=icm.getMenu();
    	  Global.Translations.TranslateMenuItem(IconMenu, R.id.miSolver, "Solver");
    	  Global.Translations.TranslateMenuItem(IconMenu, R.id.miNotes, "Notes");
    	  Global.Translations.TranslateMenuItem(IconMenu, R.id.miDescription, "Description");
    	  Global.Translations.TranslateMenuItem(IconMenu, R.id.miWaypoints, "Waypoints");
    	  icm.show();
      } 
  	  else if (v == buttonMap)
      {
    	icm = new IconContextMenu(this, R.menu.menu_map);
		icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() 
		{
			
			@Override
			public void onIconContextItemSelected(MenuItem item, Object info) 
			{
				switch (item.getItemId())
		    	{
				// Nav
		    	case R.id.miCompassView:
		    		showView(compassView);
		    		
		    		break;
		    	
		    	case R.id.miMapView:
		    		showView(mapView);
		    		
		    		break;
		    		
		    	default:
					
		    	}
		    }
		});
  	  icm.show();
  	     	 
      }
      else if (v == buttonInfo)
      {
    	 
    	icm = new IconContextMenu(this, R.menu.menu_info);
  		icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() 
  		{
  			
  			@Override
  			public void onIconContextItemSelected(MenuItem item, Object info) 
  			{
  				switch (item.getItemId())
  		    	{
  			// Info
  		    	case R.id.miLogView:
  		    		showView(logView);
  		    		break;
  		    	case R.id.miSpoilerView:
  		    		showView(spoilerView);
  		    		break;
  		    	case R.id.miHint:
  		    		if (Global.selectedCache == null)
  		    			break;
  		    		String hint = Global.selectedCache.Hint();
  		    		if (hint.equals(""))
  		    			break;
  		    		
  		    		final Intent hintIntent = new Intent().setClass(mainActivity, HintDialog.class);
  			        Bundle b = new Bundle();
  			        b.putSerializable("Hint", hint);
  			        hintIntent.putExtras(b);
  			        mainActivity.startActivity(hintIntent);
  		    		break;
  		    	case R.id.miFieldNotes:
  		    		showView(fieldNotesView);
  		    		// beim Anzeigen der FieldNotesView gleich das Optionsmen� zeigen
  		    		openOptionsMenu();
  		    		break;				
  		    	}
  		    }
  		});
    	  
    	  // Menu Item Hint enabled / disabled
    	  boolean enabled = false;
    	  if ((Global.selectedCache != null) && (!Global.selectedCache.Hint().equals("")))
    		  enabled = true;
    	  MenuItem mi = icm.menu.findItem(R.id.miHint);
    	  if (mi != null)
    		  mi.setEnabled(enabled);
    	  mi = icm.menu.findItem(R.id.miSpoilerView);
    	  if (mi != null)
    		  mi.setEnabled(Global.selectedCache.SpoilerExists());
    	  icm.show();
    	  Global.Translations.TranslateMenuItem(menu, R.id.miHint, "hint");
      } 
      else if (v == buttonMisc)
      {   		
    	    
    	icm = new IconContextMenu(this, R.menu.menu_misc);
		icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
			
			@Override
			public void onIconContextItemSelected(MenuItem item, Object info) {
				switch (item.getItemId())
		    	{
				// Misc
		    	case R.id.miScreenLock:
		    		startScreenLock();
		    		break;
		    	case R.id.miDayNight:
		    		Config.changeDayNight();
		    		ActivityUtils.changeToTheme(mainActivity,Config.GetBool("nightMode")? ActivityUtils.THEME_NIGHT : ActivityUtils.THEME_DAY );
		    		Toast.makeText(mainActivity, "changeDayNight", Toast.LENGTH_SHORT).show();
		    		break;
		    	case R.id.miSettings:
		    		final Intent mainIntent = new Intent().setClass( mainActivity, Settings.class);
		    		Bundle b = new Bundle();
  			        b.putSerializable("Show", -1);
  			        mainIntent.putExtras(b);
		    		mainActivity.startActivity(mainIntent);
		    		break;
		    		
		    	case R.id.miCompassView:
		    		showView(compassView);
		    		
		    		break;
		    		
		    	case R.id.miVoiceRecorder:
		    		if (!VoiceRecIsStart) // Voice Recorder starten
		    		{
			            Log.d("DroidCachebox", "Starting voice recorder on the phone...");
			            
			    		//define the file-name to save voice taken by activity
			            String directory = Config.GetString("UserImageFolder");
			            if (!Global.DirectoryExists(directory))
			            {
			                Log.d("DroidCachebox", "Media-Folder does not exist...");
			                break;
			            }
			           
			            basename = Global.GetDateTimeString();
			            
			            if (Global.selectedCache != null)
			            {
			            	String validName=Global.RemoveInvalidFatChars(Global.selectedCache.GcCode + "-" + Global.selectedCache.Name);
			            	mediaCacheName = validName.substring(0,(validName.length()>32)? 32 : validName.length());
			                //Title = Global.selectedCache.Name;
			            }
			            else
			            {
			            	mediaCacheName = "Voice";
			            }
	
			            basename += " " + mediaCacheName;
			            mediafilename = (directory + "/" + basename + ".wav");
			            
			            // Start recording
			            //extAudioRecorder = ExtAudioRecorder.getInstanse(true);	  // Compressed recording (AMR)
			            extAudioRecorder = ExtAudioRecorder.getInstanse(false); // Uncompressed recording (WAV)

			            extAudioRecorder.setOutputFile(mediafilename);
			            extAudioRecorder.prepare();
			            extAudioRecorder.start();

			            String MediaFolder = Config.GetString("UserImageFolder");
		            	String TrackFolder = Config.GetString("TrackFolder");
		            	String relativPath = Global.getRelativePath(MediaFolder, TrackFolder, "/"); 
		            	// Da eine Voice keine Momentaufnahme ist, muss die Zeit und die Koordinaten beim Start der Aufnahme verwendet werden.
		            	TrackRecorder.AnnotateMedia(basename + ".wav", relativPath + "/" + basename + ".wav", Global.LastValidPosition, Global.GetTrackDateTimeString());
			    		Toast.makeText(mainActivity, "Start Voice Recorder", Toast.LENGTH_SHORT).show();

			            VoiceRecIsStart = true;
			    		break;	
		    		}
		    		else
		    		{	// Voice Recorder stoppen
			            Log.d("DroidCachebox", "Stoping voice recorder on the phone...");
			            // Stop recording
			            extAudioRecorder.stop();
			            extAudioRecorder.release();
			            extAudioRecorder = null;
			            
			    		Toast.makeText(mainActivity, "Stop Voice Recorder", Toast.LENGTH_SHORT).show();
			            VoiceRecIsStart = false;
			            break;
		    		}
		    		
		    	case R.id.miTakePhoto:
		            Log.d("DroidCachebox", "Starting camera on the phone...");
		            
		    		//define the file-name to save photo taken by Camera activity
		            String directory = Config.GetString("UserImageFolder");
		            if (!Global.DirectoryExists(directory))
		            {
		                Log.d("DroidCachebox", "Media-Folder does not exist...");
		                break;
		            }
		           
		            basename = Global.GetDateTimeString();
		            
		            if (Global.selectedCache != null)
		            {
		            	String validName=Global.RemoveInvalidFatChars(Global.selectedCache.GcCode + "-" + Global.selectedCache.Name);
		            	mediaCacheName = validName.substring(0,(validName.length()>32)? 32 : validName.length());
		                //Title = Global.selectedCache.Name;
		            }
		            else
		            {
		            	mediaCacheName = "Image";
		            }

		            basename += " " + mediaCacheName;
		            mediafile = new File(directory + "/" + basename + ".jpg");

		    		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediafile));
		    		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		    		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

		    		break;
		    		
		    	case R.id.miRecordVideo:
		            Log.d("DroidCachebox", "Starting video on the phone...");
		            
		    		//define the file-name to save video taken by Camera activity
		            directory = Config.GetString("UserImageFolder");
		            if (!Global.DirectoryExists(directory))
		            {
		                Log.d("DroidCachebox", "Media-Folder does not exist...");
		                break;
		            }
		           
		            basename = Global.GetDateTimeString();
		            
		            if (Global.selectedCache != null)
		            {
		            	String validName=Global.RemoveInvalidFatChars(Global.selectedCache.GcCode + "-" + Global.selectedCache.Name);
		            	mediaCacheName = validName.substring(0,(validName.length()>32)? 32 : validName.length());
		                //Title = Global.selectedCache.Name;
		            }
		            else
		            {
		            	mediaCacheName = "Video";
		            }

		            basename += " " + mediaCacheName;
		            mediafile = new File(directory + "/" + basename + ".3gp");
		            
	            	// Da ein Video keine Momentaufnahme ist, muss die Zeit und die Koordinaten beim Start der Aufnahme verwendet werden.
	            	mediaTimeString = Global.GetTrackDateTimeString();
	            	mediaCoordinate = Global.LastValidPosition;
	            	
		    		ContentValues values = new ContentValues();  
		    		values.put(MediaStore.Video.Media.TITLE, "captureTemp.mp4");  
		    		cameraVideoURI = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);  

		    		final Intent videointent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		    		videointent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediafile));
		    		videointent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
		    		//videointent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, MAXIMUM_VIDEO_SIZE);               

		    		startActivityForResult(videointent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
		    		break;	
		    	
		    	case R.id.miTestEmpty:
		    		showView(TestEmpty);
		    		break;
		    		
		    	default:
					
		    	}
		    }
		});
		
		Menu IconMenu=icm.getMenu();
		Global.Translations.TranslateMenuItem(IconMenu, R.id.miSettings, "settings");
      	try
    	{
    		MenuItem mi = IconMenu.findItem(R.id.miVoiceRecorder);
    		if (mi != null)
    			if (!VoiceRecIsStart)
    				mi.setTitle("Voice Recorder");
    			else
    				mi.setTitle("Stop Voice Rec.");
    	} catch (Exception exc)
    	{ }
	  	  
	  	  icm.show();
    	 
      }
    	  
    	 
    }
    
    public boolean onContextItemSelected(MenuItem item)	 
    {     	 
    	// First check whether this is a MenuItem of a View	 
    	if ((aktView != null) && (aktView.ContextMenuItemSelected(item)))	 
    		return true;
    	return false;
    }

    public void SelectedCacheChanged(Cache cache, Waypoint waypoint)
    {
/*    	TextView textview = (TextView)this.findViewById(R.id.textCacheName);
    	textview.setText(cache.Name);*/
    }
    
       
    boolean initialResortAfterFirstFixCompleted = false;
    boolean initialFixSoundCompleted = false;
    

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		Global.Locator.setLocation(location);
		PositionEventList.Call(location);

        if (!initialResortAfterFirstFixCompleted && Global.LastValidPosition.Valid)
        {
            if (Global.SelectedCache() == null)
                Database.Data.Query.Resort();
            initialResortAfterFirstFixCompleted = true;
        }
        if (!initialFixSoundCompleted && Global.LastValidPosition.Valid)
        {
        	Global.PlaySound("GPS_Fix.wav");
        	initialFixSoundCompleted = true;
        }

		TrackRecorder.recordPosition();
        // schau die 50 n�chsten Caches durch, wenn einer davon n�her ist als der aktuell n�chste -> umsortieren und raus
        // only when showing Map or cacheList
		if (!Global.ResortAtWork)
		{
			if (Global.autoResort && ((aktView == mapView) || (aktView == cacheListView)))
			{
                int z = 0;
                if (!(Global.NearestCache() == null))
                {
                    for (Cache cache : Database.Data.Query)
                    {
                        z++;
                        if (z >= 50)
                            return;
                        if (cache.Distance() < Global.NearestCache().Distance())
                        {
                            Database.Data.Query.Resort();
                            Global.PlaySound("AutoResort.wav");
                            return;
                        }
                    }
                }
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
			Global.Locator.setLocation(null);
		if (status == LocationProvider.OUT_OF_SERVICE)
			Global.Locator.setLocation(null);
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	    Log.d("SolHunter", "Key event code "+keyCode);
	    if (keyCode == KeyEvent.KEYCODE_BACK) 
	    {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
				            //Yes button clicked
				            try {
								finish();
							} catch (Throwable e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				            break;
				        case DialogInterface.BUTTON_NEGATIVE:
				            //No button clicked
				            break;
				        }
				    }
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Close DroidCB?")
//					.setTitle(Global.Translations.Get("!DelWP"))
					.setPositiveButton(Global.Translations.Get("yes"), dialogClickListener)
				    .setNegativeButton(Global.Translations.Get("no"), dialogClickListener).show();
			return true;
    	}
    	return false;
    }
	
	public void EditCoordinate(Coordinate coord)
	{
		
	}

	
   
	@Override
	public void CacheListChangedEvent() 
	{
		//Database.Data.Query.size();
		if ((Global.LastFilter == null) || (Global.LastFilter.ToString().equals("")) || (Global.LastFilter.ToString().equals(PresetListView.presets[0])))
        {
			this.buttonDB.getBackground().clearColorFilter();
        }
        else
        {
        	this.buttonDB.getBackground().setColorFilter(Color.argb(255, 250, 128, 114), Mode.MULTIPLY); //Color.Salmon;
        };        
	}
	
 
    
    
}