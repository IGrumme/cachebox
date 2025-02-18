package de.droidcachebox;

import static android.content.Intent.ACTION_VIEW;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import de.droidcachebox.activities.CBForeground;
import de.droidcachebox.activities.GcApiLogin;
import de.droidcachebox.core.FilterInstances;
import de.droidcachebox.core.FilterProperties;
import de.droidcachebox.database.CBDB;
import de.droidcachebox.database.SQLiteClass;
import de.droidcachebox.database.SQLiteInterface;
import de.droidcachebox.ex_import.GPXFileImporter;
import de.droidcachebox.ex_import.ImportProgress;
import de.droidcachebox.ex_import.Importer;
import de.droidcachebox.gdx.GL;
import de.droidcachebox.gdx.activities.EditFilterSettings;
import de.droidcachebox.gdx.controls.animation.WorkAnimation;
import de.droidcachebox.gdx.controls.dialogs.ButtonDialog;
import de.droidcachebox.gdx.controls.dialogs.CancelWaitDialog;
import de.droidcachebox.gdx.controls.dialogs.MsgBoxButton;
import de.droidcachebox.gdx.controls.dialogs.MsgBoxIcon;
import de.droidcachebox.gdx.controls.dialogs.RunAndReady;
import de.droidcachebox.gdx.controls.popups.SearchDialog;
import de.droidcachebox.locator.CBLocation;
import de.droidcachebox.locator.Coordinate;
import de.droidcachebox.locator.CoordinateGPS;
import de.droidcachebox.locator.GPS;
import de.droidcachebox.locator.GpsStateChangeEventList;
import de.droidcachebox.locator.GpsStrength;
import de.droidcachebox.locator.Locator;
import de.droidcachebox.locator.map.MapTileLoader;
import de.droidcachebox.menu.Action;
import de.droidcachebox.menu.ViewManager;
import de.droidcachebox.menu.menuBtn3.ShowMap;
import de.droidcachebox.menu.menuBtn3.executes.FZKDownload;
import de.droidcachebox.menu.menuBtn5.ShowSettings;
import de.droidcachebox.menu.quickBtns.ShowSearchDialog;
import de.droidcachebox.settings.SettingBase;
import de.droidcachebox.settings.SettingBool;
import de.droidcachebox.settings.SettingInt;
import de.droidcachebox.settings.SettingString;
import de.droidcachebox.settings.Settings;
import de.droidcachebox.translation.Translation;
import de.droidcachebox.utils.AbstractFile;
import de.droidcachebox.utils.CB_List;
import de.droidcachebox.utils.FileFactory;
import de.droidcachebox.utils.IChanged;
import de.droidcachebox.utils.StringReturner;
import de.droidcachebox.utils.log.Log;

public class AndroidPlatformMethods implements Platform.PlatformMethods, LocationListener {
    private static final String sClass = "PlatformListener";
    private static final int REQUEST_GET_APIKEY = 987654321;
    private static final int ACTION_OPEN_DOCUMENT_TREE = 6518;
    private static final int ACTION_OPEN_DOCUMENT = 6519;
    private static final int useGNSS = Build.VERSION_CODES.N;
    private final AndroidApplication androidApplication;
    private final Activity mainActivity;
    private final Main mainMain;
    private final String defaultBrowserPackageName;
    private final CB_List<GpsStrength> coreSatList = new CB_List<>(14);
    private AtomicBoolean torchAvailable = null;
    private Camera deviceCamera;
    private SharedPreferences androidSetting;
    private SharedPreferences.Editor androidSettingEditor;
    private AndroidEventListener handlingGetApiAuth;
    private LocationManager locationManager;
    private AndroidEventListener handlingGetDirectoryAccess, handlingGetDocumentAccess;
    private Intent locationServiceIntent;
    private GnssStatus.Callback gnssStatusCallback;
    private android.location.GpsStatus.Listener gpsStatusListener;
    private boolean lostCheck = false;
    private boolean askForLocationPermission;

    AndroidPlatformMethods(Main main) {
        String defaultBrowserPackageName1 = "android";
        androidApplication = main;
        mainActivity = main;
        mainMain = main;
        OnResumeListeners.getInstance().addListener(this::handleExternalRequest);
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        try {
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            defaultBrowserPackageName1 = resolveInfo.activityInfo.packageName;
        } catch (Exception ignored) {
        }
        defaultBrowserPackageName = defaultBrowserPackageName1;
        IChanged handleAllowLocationServiceConfigChanged = this::changeLocationService;
        Settings.allowLocationService.addSettingChangedListener(handleAllowLocationServiceConfigChanged);
        IChanged handleGpsUpdateTimeConfigChanged = () -> {
            int updateTime1 = Settings.gpsUpdateTime.getValue();
            try {
                getLocationManager().requestLocationUpdates(GPS_PROVIDER, updateTime1, 1, this);
            } catch (SecurityException sex) {
                Log.err(sClass, "SettingsClass.gpsUpdateTime changed: " + sex.getLocalizedMessage());
            }
        };
        Settings.gpsUpdateTime.addSettingChangedListener(handleGpsUpdateTimeConfigChanged);

        if (Build.VERSION.SDK_INT >= useGNSS) {
            gnssStatusCallback = new GnssStatus.Callback() {

                @Override
                public void onSatelliteStatusChanged(final GnssStatus status) {
                    final int satellites = status.getSatelliteCount();
                    int fixed = 0;
                    coreSatList.clear();
                    for (int satelliteNr = 0; satelliteNr < satellites; satelliteNr++) {
                        if (status.usedInFix(satelliteNr)) {
                            fixed++;
                            coreSatList.add(new GpsStrength(true, satelliteNr));
                        } else {
                            coreSatList.add(new GpsStrength(false, satelliteNr));
                        }
                    }
                    coreSatList.sort();
                    GPS.setSatFixes(fixed);
                    GPS.setSatVisible(satellites);
                    GPS.setSatList(coreSatList);
                    GpsStateChangeEventList.Call();
                    if (fixed < 1 && (Locator.getInstance().isFixed())) {
                        if (!lostCheck) {
                            Timer timer = new Timer();
                            TimerTask task = new TimerTask() {
                                @Override
                                public void run() {
                                    if (GPS.getFixedSats() < 1)
                                        Locator.getInstance().FallBack2Network();
                                    lostCheck = false;
                                }
                            };
                            timer.schedule(task, 1000);
                        }
                    }
                }

                @Override
                public void onStarted() {
                    // GPS has startet
                    Log.debug(sClass, "Gnss started");
                }

                @Override
                public void onStopped() {
                    // GPS has stopped
                    Log.debug(sClass, "Gnss stopped");
                }
            };
        } else {
            gpsStatusListener = event -> {

                if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS || event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    final GpsStatus status = getLocationManager().getGpsStatus(null);

                    int satellites = 0;
                    int fixed = 0;
                    coreSatList.clear();
                    for (final GpsSatellite satellite : status.getSatellites()) {
                        satellites++;
                        if (satellite.usedInFix()) {
                            fixed++;
                            coreSatList.add(new GpsStrength(true, satellite.getSnr()));
                        } else {
                            coreSatList.add(new GpsStrength(false, satellite.getSnr()));
                        }
                    }

                    coreSatList.sort();
                    GPS.setSatFixes(fixed);
                    GPS.setSatVisible(satellites);
                    GPS.setSatList(coreSatList);
                    GpsStateChangeEventList.Call();
                    if (fixed < 1 && (Locator.getInstance().isFixed())) {
                        if (!lostCheck) {
                            Timer timer = new Timer();
                            TimerTask task = new TimerTask() {
                                @Override
                                public void run() {
                                    if (GPS.getFixedSats() < 1)
                                        Locator.getInstance().FallBack2Network();
                                    lostCheck = false;
                                }
                            };
                            timer.schedule(task, 1000);
                        }
                    }
                }
            };
        }

    }

    @Override
    public void writePlatformSetting(SettingBase<?> setting) {
        if (androidSetting == null)
            androidSetting = mainActivity.getSharedPreferences(Global.PreferencesNAME, 0);
        if (androidSettingEditor == null)
            androidSettingEditor = androidSetting.edit();
        if (setting instanceof SettingBool) {
            androidSettingEditor.putBoolean(setting.getName(), ((SettingBool) setting).getValue());
        } else if (setting instanceof SettingString) {
            androidSettingEditor.putString(setting.getName(), ((SettingString) setting).getValue());
        } else if (setting instanceof SettingInt) {
            androidSettingEditor.putInt(setting.getName(), ((SettingInt) setting).getValue());
        }
        androidSettingEditor.apply();
    }

    @Override
    public void readPlatformSetting(SettingBase<?> setting) {
        if (androidSetting == null)
            androidSetting = mainActivity.getSharedPreferences(Global.PreferencesNAME, 0);
        if (setting instanceof SettingString) {
            String value = androidSetting.getString(setting.getName(), ((SettingString) setting).getDefaultValue());
            ((SettingString) setting).setValue(value);
        } else if (setting instanceof SettingBool) {
            boolean value = androidSetting.getBoolean(setting.getName(), ((SettingBool) setting).getDefaultValue());
            ((SettingBool) setting).setValue(value);
        } else if (setting instanceof SettingInt) {
            int value = androidSetting.getInt(setting.getName(), ((SettingInt) setting).getDefaultValue());
            ((SettingInt) setting).setValue(value);
        }
        setting.clearDirty();
    }

    @Override
    public boolean isOnline() {
        // isOnline Liefert TRUE wenn die Möglichkeit besteht auf das Internet zuzugreifen
        ConnectivityManager cm = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        }
        return false;
    }

    @Override
    public boolean isGPSon() {
        boolean ret = getLocationManager().isProviderEnabled(GPS_PROVIDER);
        if (!ret && Settings.Ask_Switch_GPS_ON.getValue())
            mainActivity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)); // dialog gps ein
        return ret;
    }

    @Override
    public void vibrate() {
        if (Settings.vibrateFeedback.getValue())
            ((Vibrator) Objects.requireNonNull(mainActivity.getSystemService(Context.VIBRATOR_SERVICE))).vibrate(Settings.VibrateTime.getValue());
    }

    @Override
    public boolean isTorchAvailable() {
        if (torchAvailable == null) {
            torchAvailable = new AtomicBoolean();
            torchAvailable.set(mainActivity.getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH));
        }
        return torchAvailable.get();
    }

    @Override
    public boolean isTorchOn() {
        return deviceCamera != null;
    }

    @Override
    public void switchTorch() {
        if (deviceCamera == null) {
            deviceCamera = Camera.open();
            Camera.Parameters p = deviceCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            deviceCamera.setParameters(p);
            deviceCamera.startPreview();
        } else {
            deviceCamera.stopPreview();
            deviceCamera.release();
            deviceCamera = null;
        }
    }

    @Override
    public void switchToGpsMeasure() {
        try {
            Log.debug(sClass, "switchToGpsMeasure()");
            int updateTime = Settings.gpsUpdateTime.getValue();
            getLocationManager().requestLocationUpdates(GPS_PROVIDER, updateTime, 0, this);
        } catch (SecurityException sex) {
            Log.err(sClass, "switchToGpsMeasure: ", sex);
        }
    }

    @Override
    public void switchToGpsDefault() {
        try {
            Log.debug(sClass, "switchtoGpsDefault()");
            int updateTime = Settings.gpsUpdateTime.getValue();
            getLocationManager().requestLocationUpdates(GPS_PROVIDER, updateTime, 1, this);
        } catch (SecurityException sex) {
            Log.err(sClass, "switchtoGpsDefault: " + sex.getLocalizedMessage());
        }
    }

    @Override
    public void getApiKey() {
        Intent intent = new Intent().setClass(mainActivity, GcApiLogin.class);
        if (handlingGetApiAuth == null)
            handlingGetApiAuth = (requestCode, resultCode, data) -> {
                androidApplication.removeAndroidEventListener(handlingGetApiAuth);
                if (requestCode == REQUEST_GET_APIKEY) {
                    ShowSettings.getInstance().returnFromFetchingApiKey(); // to view the new setting
                }
            };
        androidApplication.addAndroidEventListener(handlingGetApiAuth);
        mainActivity.startActivityForResult(intent, REQUEST_GET_APIKEY);
    }

    @Override
    public void callUrl(String url) {
        try {
            url = url.trim();
            if (url.startsWith("www.")) {
                url = "https://" + url;
            }
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(ACTION_VIEW);
            intent.setDataAndType(uri, "text/html");
            if (defaultBrowserPackageName.equals("android")) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // The BROWSABLE category is required to get links from web pages.
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
            } else {
                intent.setPackage(defaultBrowserPackageName);
            }
            if (intent.resolveActivity(mainActivity.getPackageManager()) != null) {
                Log.debug(sClass, "Start activity for " + uri.toString());
            } else {
                Log.err(sClass, "Activity for " + url + " not visible. (" + defaultBrowserPackageName + "). Try startActivity(intent) anyway.");
                // Toast.makeText(mainActivity, Translation.get("Cann_not_open_cache_browser") + " (" + url + ")", Toast.LENGTH_LONG).show();
                // start independent from visibility ( Android 11 hides, even if invisible, a browser starts! )
            }
            mainActivity.startActivity(intent);
        } catch (Exception ex) {
            Log.err(sClass, Translation.get("Cann_not_open_cache_browser") + " (" + url + ")", ex);
        }
    }

    @Override
    public void startPictureApp(String fileName) {
        Uri uriToImage = Uri.fromFile(new java.io.File(fileName));
        Intent shareIntent = new Intent(ACTION_VIEW);
        shareIntent.setDataAndType(uriToImage, "image/*");
        mainActivity.startActivity(Intent.createChooser(shareIntent, mainActivity.getResources().getText(R.string.app_name)));
    }

    @Override
    public SQLiteInterface createSQLInstance() {
        return new SQLiteClass(mainActivity);
    }

    @Override
    public void quit() {
        if (GlobalCore.isSetSelectedCache()) {
            // speichere selektierten Cache, da nicht alles über die
            // SelectedCacheEventList läuft
            Settings.lastSelectedCache.setValue(GlobalCore.getSelectedCache().getGeoCacheCode());
            Log.debug(sClass, "LastSelectedCache = " + GlobalCore.getSelectedCache().getGeoCacheCode());
        }
        MapTileLoader.getInstance().stopQueueProzessors();
        Settings.getInstance().acceptChanges();
        CBDB.getInstance().close();
        mainActivity.finish();
    }

    @Override
    public void handleExternalRequest() {
        // viewmanager must have been initialized
        final Bundle extras = mainActivity.getIntent().getExtras();
        if (extras != null) {
            Log.debug(sClass, "prepared Request from splash");
            if (ViewManager.that.isInitialized()) {
                String externalRequestGCCode = extras.getString("GcCode");
                if (externalRequestGCCode != null) {
                    Log.debug(sClass, "importCacheByGCCode");
                    mainActivity.getIntent().removeExtra("GcCode");
                    importCacheByGCCode(externalRequestGCCode);
                }
                String externalRequestGpxPath = extras.getString("GpxPath");
                if (externalRequestGpxPath != null) {
                    Log.debug(sClass, "externalRequestGpxPath " + externalRequestGpxPath);
                    mainActivity.getIntent().removeExtra("GpxPath");
                    if (externalRequestGpxPath.endsWith(".map")) {
                        AbstractFile sourceFile = FileFactory.createFile(externalRequestGpxPath);
                        AbstractFile destinationFile = FileFactory.createFile(Settings.getInstance().getPathForMapFile(), sourceFile.getName());
                        boolean result = sourceFile.renameTo(destinationFile);
                        String sResult = result ? " ok!" : " no success!";
                        if (result) {
                            Log.debug(sClass, "Move map-file " + destinationFile.getPath() + sResult);
                        } else {
                            Log.err(sClass, "Move map-file " + destinationFile.getPath() + sResult);
                        }
                    } else {
                        Log.debug(sClass, "importGPXFile (*.gpx or *.zip)");
                        importGPXFile(externalRequestGpxPath);
                    }
                } else {
                    Log.debug(sClass, "externalRequestGpxPath null");
                }
                Log.debug(sClass, "externalRequestGuid start");
                String externalRequestGuid = extras.getString("Guid");
                if (externalRequestGuid != null) {
                    Log.debug(sClass, "importCacheByGuid");
                    mainActivity.getIntent().removeExtra("Guid");
                    importCacheByGuid();
                }
                Log.debug(sClass, "externalRequestLatLon start");
                String externalRequestLatLon = extras.getString("LatLon");
                if (externalRequestLatLon != null) {
                    Log.debug(sClass, "positionLatLon");
                    mainActivity.getIntent().removeExtra("LatLon");
                    positionLatLon(externalRequestLatLon);
                }
                Log.debug(sClass, "externalRequestMapDownloadPath start");
                String externalRequestMapDownloadPath = extras.getString("MapDownloadPath");
                if (externalRequestMapDownloadPath != null) {
                    Log.debug(sClass, "MapDownload");
                    mainActivity.getIntent().removeExtra("MapDownloadPath");
                    FZKDownload fzkDownload = new FZKDownload();
                    fzkDownload.showImportByUrl(externalRequestMapDownloadPath);
                }
                Log.debug(sClass, "externalRequestName start");
                String externalRequestName = extras.getString("Name");
                if (externalRequestName != null) {
                    Log.debug(sClass, "importCacheByName");
                    mainActivity.getIntent().removeExtra("Name");
                    importCacheByName();
                }
                Log.debug(sClass, "externalRequest end handle");
            }
        }
    }

    @Override
    public String removeHtmlEntyties(String text) {
        return HtmlCompat.fromHtml(text, FROM_HTML_MODE_LEGACY).toString();
    }

    @Override
    public String getFileProviderContentUrl(String localFileName) {
        return FileProvider.getUriForFile(mainActivity, "de.droidcachebox.android.fileprovider", new File(localFileName)).toString();
    }

    @Override
    public void getDirectoryAccess(String _DirectoryToAccess, StringReturner stringReturner) {
        // Choose a directory using the system's file picker.
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            // Optionally, specify a URI for the directory that should be opened in the system file picker when it loads.
            // DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            // intent.putExtra(EXTRA_INITIAL_URI, documentFile.getUri());
            // intent.putExtra(DocumentsContract.EXTRA_INFO, "please select a dir for " + _DirectoryToAccess);
            if ((android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.Q) || (intent.resolveActivity(mainActivity.getPackageManager()) != null)) {
                if (handlingGetDirectoryAccess == null)
                    handlingGetDirectoryAccess = (requestCode, resultCode, resultData) -> {
                        androidApplication.removeAndroidEventListener(handlingGetDirectoryAccess);
                        if (requestCode == ACTION_OPEN_DOCUMENT_TREE) {
                            if (resultCode == Activity.RESULT_OK) {
                                // The result data contains an Uri for the directory that the user selected.
                                if (resultData != null) {
                                    stringReturner.returnString(resultData.getData().toString());
                                }
                            }
                        }
                    };
                androidApplication.addAndroidEventListener(handlingGetDirectoryAccess);
                try {
                    mainActivity.startActivityForResult(intent, ACTION_OPEN_DOCUMENT_TREE);
                } catch (ActivityNotFoundException ex) {
                    androidApplication.removeAndroidEventListener(handlingGetDirectoryAccess);
                    Log.err(sClass, "PackageManager: No activity found for intent ACTION_OPEN_DOCUMENT_TREE: " + intent);
                }
            } else {
                Log.err(sClass, "PackageManager: No activity found for intent ACTION_OPEN_DOCUMENT_TREE: " + intent);
            }
        }
    }

    public void getDocumentAccess(String _DirectoryToAccess, StringReturner stringReturner) {
        // Choose a directory using the system's file picker.
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*"); // intent.setType("application/octet-stream");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Optionally, specify a URI for the directory that should be opened in the system file picker when it loads.
                if (_DirectoryToAccess.startsWith("content:")) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(_DirectoryToAccess));
                }
            }
            if ((android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.Q) || (intent.resolveActivity(mainActivity.getPackageManager()) != null)) {
                if (handlingGetDocumentAccess == null) {
                    Log.debug(sClass, "create resulthandler for Filepicker");
                    handlingGetDocumentAccess = (requestCode, resultCode, resultData) -> {
                        androidApplication.removeAndroidEventListener(handlingGetDocumentAccess);
                        if (requestCode == ACTION_OPEN_DOCUMENT) {
                            if (resultCode == Activity.RESULT_OK) {
                                // The result data contains an Uri for the file(document) that the user selected.
                                if (resultData != null) {
                                    // Perform actions on result
                                    Uri uri = resultData.getData();
                                    mainActivity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    stringReturner.returnString(uri.toString());
                                } else {
                                    Log.debug(sClass, "Filepicker resultData = null (nothing selected?)");
                                }
                            } else {
                                Log.debug(sClass, "Filepicker without result");
                            }
                        }
                    };
                }
                Log.debug(sClass, "Start Filepicker");
                androidApplication.addAndroidEventListener(handlingGetDocumentAccess);
                try {
                    mainActivity.startActivityForResult(intent, ACTION_OPEN_DOCUMENT);
                } catch (ActivityNotFoundException ex) {
                    Log.debug(sClass, "PackageManager: No activity found for intent ACTION_OPEN_DOCUMENT: " + intent);
                    androidApplication.removeAndroidEventListener(handlingGetDocumentAccess);
                    getDirectoryAccess(_DirectoryToAccess, stringReturner);
                }
            } else {
                Log.debug(sClass, "PackageManager: No activity found for intent ACTION_OPEN_DOCUMENT: " + intent);
                getDirectoryAccess(_DirectoryToAccess, stringReturner);
            }
        }
    }

    @Override
    public InputStream getInputStream(String path) throws FileNotFoundException {
        try {
            return mainActivity.getContentResolver().openInputStream(Uri.parse(path));
        } catch (Exception ex) {
            Log.err(sClass, path, ex);
            throw new FileNotFoundException("Can't get Input Stream!");
        }
    }

    @Override
    public OutputStream getOutputStream(String contentFile) throws FileNotFoundException {
        try {
            return mainActivity.getContentResolver().openOutputStream(Uri.parse(contentFile));
        } catch (Exception ex) {
            Log.err(sClass, contentFile, ex);
            throw new FileNotFoundException("Can't get Output Stream!");
        }
    }

    @Override
    public boolean request_getLocationIfInBackground() {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                String permissionlabel = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    permissionlabel = mainActivity.getPackageManager().getBackgroundPermissionOptionLabel().toString();
                }

                // hint & explanation
                ButtonDialog bd = new ButtonDialog(
                        Translation.get("PleaseConfirm") + "\n\n" + permissionlabel + "\n\n" + Translation.get("GPSDisclosureText"),
                        Translation.get("GPSDisclosureTitle"),
                        MsgBoxButton.YesNo,
                        MsgBoxIcon.Information
                );
                bd.setButtonClickHandler((btnNumber, data) -> {
                    if (btnNumber == ButtonDialog.BTN_LEFT_POSITIVE) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            final String[] requestedPermissions = new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION};
                            ActivityCompat.requestPermissions(mainActivity, requestedPermissions, Main.Request_getLocationIfInBackground);
                        } else {
                            // frage trotzdem, aber es popt nicht mehr auf. Daher
                            mainActivity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)); // dialog gps ein
                        }
                    }
                            /*
                            else {
                                // if you don't want
                            }

                             */
                    return true; // click is handled
                });
                bd.show();
                return false;
            }
        }
        return true;
    }

    @Override
    public int getCacheCountInDB(String absolutePath) {
        try {
            SQLiteDatabase myDB = SQLiteDatabase.openDatabase(absolutePath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = myDB.rawQuery("select count(*) from Caches", null);
            c.moveToFirst();
            int count = c.getInt(0);
            c.close();
            myDB.close();
            return count;
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void positionLatLon(String externalRequestLatLon) {
        String[] s;
        try {
            s = externalRequestLatLon.split(",");
            Coordinate coordinate = new Coordinate(Double.parseDouble(s[0]), Double.parseDouble(s[1]));
            Log.debug(sClass, "" + externalRequestLatLon + " " + s[0] + " , " + s[1] + "\n" + coordinate);
            if (coordinate.isValid()) {
                Action.ShowMap.action.execute();
                ((ShowMap) Action.ShowMap.action).getNormalMapView().setBtnMapStateToFree(); // btn
                // ShowMap.getInstance().normalMapView.setMapState(MapViewBase.MapState.FREE);
                ((ShowMap) Action.ShowMap.action).getNormalMapView().setCenter(new CoordinateGPS(coordinate.latitude, coordinate.longitude));
            }
        } catch (Exception ignored) {
        }
    }

    private void importCacheByGuid() {
    }

    private void importCacheByGCCode(final String externalRequestGCCode) {
        TimerTask runTheSearchTasks = new TimerTask() {
            @Override
            public void run() {
                if (externalRequestGCCode != null) {
                    mainActivity.runOnUiThread(() -> {
                        Action.ShowSearchDialog.action.execute();
                        ((ShowSearchDialog) Action.ShowSearchDialog.action).doSearchOnline(externalRequestGCCode, SearchDialog.SearchMode.GcCode);
                    });
                }
            }
        };
        new Timer().schedule(runTheSearchTasks, 500);
    }

    private void importGPXFile(final String externalRequestGpxPath) {
        Date ImportStart = new Date();
        AtomicBoolean isCanceled = new AtomicBoolean(false);
        TimerTask gpxImportTask = new TimerTask() {
            @Override
            public void run() {
                Log.debug(sClass, "ImportGPXFile");
                mainActivity.runOnUiThread(() -> new CancelWaitDialog(Translation.get("ImportGPX"), new WorkAnimation(), new RunAndReady() {
                    @Override
                    public void ready() {
                        FilterProperties props = FilterInstances.getLastFilter();
                        EditFilterSettings.applyFilter(props);

                        long ImportZeit = new Date().getTime() - ImportStart.getTime();
                        String msg = "Import " + GPXFileImporter.CacheCount + "Caches\n" + GPXFileImporter.LogCount + "Logs\n in " + ImportZeit;
                        Log.debug(sClass, msg.replace("\n", "\n\r") + " from " + externalRequestGpxPath);
                        GL.that.toast(msg);
                    }

                    @Override
                    public void run() {
                        Log.debug(sClass, "Import GPXFile from " + externalRequestGpxPath + " started");

                        CBDB.getInstance().beginTransaction();
                        try {
                            Importer importer = new Importer();
                            importer.importGpx(externalRequestGpxPath,
                                    new ImportProgress((message, progressMessage, progress) -> {
                                    }), isCanceled::get);
                        } catch (Exception ignored) {
                        }
                        CBDB.getInstance().setTransactionSuccessful();
                        CBDB.getInstance().endTransaction();
                    }

                    @Override
                    public void setIsCanceled() {
                        isCanceled.set(true);
                    }

                }).show());
            }
        };

        new Timer().schedule(gpxImportTask, 500);
    }

    private void importCacheByName() {
    }

    LocationManager getLocationManager() {
        return getLocationManager(false);
    }

    LocationManager getLocationManager(boolean forceInitialization) {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
            askForLocationPermission = true;
            return locationManager;
        }
        askForLocationPermission = false;
        if (locationManager == null || forceInitialization) {
            Log.debug(sClass, "Initialise  location manager");

            // Get the (gps) location manager
            locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);

            /*
            Longri: Ich habe die Zeiten und Distanzen der Locationupdates angepasst.
            Der Network Provider hat eine schlechte Genauigkeit,
            daher reicht es wenn er alle 10sec einen wert liefert und der alte um 300m abweicht.
            Beim GPS Provider habe ich die Aktualisierungszeit verkürzt.
            Bei deaktiviertem Hardware Kompass bleiben trotzdem die Werte noch in einem gesunden Verhältnis zwischen Performance und Stromverbrauch.
            Andere apps haben hier 0.
             */

            int updateTime = Settings.gpsUpdateTime.getValue();

            try {
                locationManager.requestLocationUpdates(GPS_PROVIDER, updateTime, 1, this);
                locationManager.requestLocationUpdates(NETWORK_PROVIDER, 10000, 300, this);
                locationManager.addNmeaListener(mainMain); // for altitude correction: removed after first achieve (onNmeaReceived in main)

                if (Build.VERSION.SDK_INT >= useGNSS) {
                    locationManager.registerGnssStatusCallback(gnssStatusCallback);
                } else {
                    locationManager.addGpsStatusListener(gpsStatusListener);
                }

            } catch (SecurityException ex) {
                Log.err(sClass, "SettingsClass.gpsUpdateTime changed: ", ex);
            }
        }
        return locationManager;
    }

    void removeFromGPS() {
        if (Build.VERSION.SDK_INT >= useGNSS) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        } else {
            locationManager.removeGpsStatusListener(gpsStatusListener);
        }
    }

    boolean askForLocationPermission() {
        return askForLocationPermission;
    }

    void resetAskForLocationPermission() {
        askForLocationPermission = false;
    }

    void startService() {
        if (Settings.allowLocationService.getValue()) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final String[] requestedPermissions = new String[]{Manifest.permission.FOREGROUND_SERVICE};
                    ActivityCompat.requestPermissions(mainActivity, requestedPermissions, Main.Request_ServiceOption);
                } else {
                    Log.err(sClass, "No Permission needed for FOREGROUND_SERVICE from SDK_INT == 26 and 27");
                    serviceCanBeStarted();
                }
            } else {
                serviceCanBeStarted();
            }
        }
    }

    void serviceCanBeStarted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            locationServiceIntent = new Intent(androidApplication, CBForeground.class);
            androidApplication.startForegroundService(locationServiceIntent);
        } else {
            Log.debug(sClass, "FOREGROUND_SERVICE requires SDK_INT >= 26");
        }
    }

    void stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.allowLocationService.getValue()) {
                androidApplication.stopService(locationServiceIntent);
            }
        }
    }

    private void changeLocationService() {
        if (Settings.allowLocationService.getValue()) {
            startService();
        } else {
            stopService();
        }
    }

    @Override
    public void onLocationChanged(Location receivedLocation) {
        // is fired from Android LocationListener: see getLocationManager in AndroidUIBaseMethods
        CBLocation.ProviderType provider;
        if (receivedLocation.getProvider().toLowerCase(new Locale("en")).contains("gps"))
            provider = CBLocation.ProviderType.GPS;
        else if (receivedLocation.getProvider().toLowerCase(new Locale("en")).contains("network"))
            provider = CBLocation.ProviderType.Network;
        else {
            provider = CBLocation.ProviderType.NULL;
        }
        Locator.getInstance().setNewLocation(new CBLocation(
                receivedLocation.getLatitude(),
                receivedLocation.getLongitude(),
                receivedLocation.getAccuracy(),
                receivedLocation.hasSpeed(),
                receivedLocation.getSpeed(),
                receivedLocation.hasBearing(),
                receivedLocation.getBearing(),
                receivedLocation.getAltitude(),
                provider));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ist obsolete, aber braucht eine leere Implementierung
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }
}
