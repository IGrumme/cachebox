package CB_UI;
/*
 * Copyright (C) 2014 team-cachebox.de
 *
 * Licensed under the : GNU General Public License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import CB_Core.CB_Core_Settings;
import CB_Locator.LocatorSettings;
import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.settings.CB_UI_Base_Settings;
import CB_Utils.Config_Core;
import CB_Utils.Log.Log;
import cb_rpc.Settings.CB_Rpc_Settings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Config extends Config_Core implements CB_Core_Settings, CB_UI_Settings, CB_UI_Base_Settings, CB_Rpc_Settings, LocatorSettings {
    private static final String log = "Config";
    public static SettingsClass settings;
    public static String ConfigName = "";
    static HashMap<String, String> keyLookup = null;
    static boolean initialized = false;
    private static Config that;

    public Config(String workPath) {
        super(workPath);
        that = this;
    }

    public static void Initialize(String workPath, String configName) {
        mWorkPath = workPath;
        ConfigName = configName;
        settings = new SettingsClass();
    }

    public static String GetString(String key) {
        checkInitialization();

        String value = keyLookup.get(key);
        if (value == null)
            return "";
        else
            return value;
    }

    public static double GetDouble(String key) {
        checkInitialization();

        String value = keyLookup.get(key);
        if (value == null)
            return 0;
        else
            return Double.parseDouble(value);
    }

    public static float GetFloat(String key) {
        checkInitialization();

        String value = keyLookup.get(key);
        if (value == null)
            return 0;
        else
            return Float.parseFloat(value);
    }

    public static Boolean GetBool(String key) {
        checkInitialization();

        String value = keyLookup.get(key);
        if (value == null)
            return false;
        else
            return Boolean.parseBoolean(value);
    }

    public static int GetInt(String key) {
        checkInitialization();

        String value = keyLookup.get(key);
        if (value == null) {
            return -1;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
            }
            return -1;
        }
    }

    public static void changeDayNight() {
        Boolean value = SettingsClass.nightMode.getValue();
        value = !value;
        SettingsClass.nightMode.setValue(value);
        Config.AcceptChanges();
    }

    public static void readConfigFile() {
        initialized = false;
        checkInitialization();
    }

    static void checkInitialization() {
        if (initialized)
            return;

        initialized = true;

        keyLookup = new HashMap<String, String>();

        BufferedReader Filereader;

        try {
            Filereader = new BufferedReader(new FileReader(ConfigName));
            String line;

            while ((line = Filereader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }

                String key = line.substring(0, idx);
                String value = line.substring(idx + 1)/* .replace("//","/" ) */;
                keyLookup.put(key, value);
            }

            Filereader.close();
        } catch (IOException e) {
            Log.err(log, "ReadConfig", "Error when accessing cachebox.config!", e);
        }

        validateDefaultConfigFile();
    }

    public static void validateDefaultConfigFile() {
        validateSetting("LanguagePath", mWorkPath + "/data/lang");
        validateSetting("Sel_LanguagePath", mWorkPath + "/data/lang/en.lan");
        validateSetting("DatabasePath", mWorkPath + "/cachebox.db3");
        validateSetting("TileCacheFolder", mWorkPath + "/cache");
        validateSetting("PocketQueryFolder", mWorkPath + "/PocketQuery");
        validateSetting("DescriptionImageFolder", mWorkPath + "/repository/images");
        validateSetting("MapPackFolder", mWorkPath + "/repository/maps");
        validateSetting("SpoilerFolder", mWorkPath + "/repository/spoilers");
        validateSetting("UserImageFolder", mWorkPath + "/User/Media");
        validateSetting("TrackFolder", mWorkPath + "/User/Tracks");

        validateSetting("FieldNotesGarminPath", mWorkPath + "/User/geocache_visits.txt");

        validateSetting("SaveFieldNotesHtml", "true");

        validateSetting("Proxy", "");
        validateSetting("ProxyPort", "");
        validateSetting("DopMin", "0.2");
        validateSetting("DopWidth", "1");
        validateSetting("OsmDpiAwareRendering", "true");
        validateSetting("LogMaxMonthAge", "99999");
        validateSetting("LogMinCount", "99999");
        validateSetting("MapInitLatitude", "-1000");
        validateSetting("MapInitLongitude", "-1000");
        validateSetting("AllowInternetAccess", "true");
        validateSetting("AllowRouteInternet", "true");
        validateSetting("ImportGpx", "true");
        validateSetting("CacheMapData", "false");
        validateSetting("CacheImageData", "false");
        validateSetting("OsmMinLevel", "8");
        validateSetting("OsmMaxLevel", "21");
        validateSetting("OsmCoverage", "1000");
        validateSetting("SuppressPowerSaving", "true");
        validateSetting("PlaySounds", "true");
        validateSetting("PopSkipOutdatedGpx", "true");
        validateSetting("MapHideMyFinds", "false");
        validateSetting("MapShowRating", "true");
        validateSetting("MapShowDT", "true");
        validateSetting("MapShowTitles", "true");
        validateSetting("ShowKeypad", "true");
        validateSetting("FoundOffset", "0");
        validateSetting("CurrentMapLayer", "Mapnik");
        validateSetting("AutoUpdate", "http://www.getcachebox.net/latest-stable");
        validateSetting("NavigationProvider", "http://openrouteservice.org/php/OpenLSRS_DetermineRoute.php");
        validateSetting("TrackRecorderStartup", "false");
        validateSetting("MapShowCompass", "true");
        validateSetting("FoundTemplate", "<br>###finds##, ##time##, Found it with DroidCachebox!");
        validateSetting("DNFTemplate", "<br>##time##. Logged it with DroidCachebox!");
        validateSetting("NeedsMaintenanceTemplate", "Logged it with DroidCachebox!");
        validateSetting("AddNoteTemplate", "Logged it with DroidCachebox!");
        validateSetting("ResortRepaint", "false");
        validateSetting("TrackDistance", "3");
        validateSetting("SoundApproachDistance", "50");
        // validateSetting("Filter", PresetListView.presets[0].toString());
        validateSetting("ZoomCross", "16");
        // validateSetting("TomTomExportFolder", Global.AppPath + "/user");
        validateSetting("GCAdditionalImageDownload", "false");
        validateSetting("GCRequestDelay", "10");

        validateSetting("MultiDBAsk", "true");
        validateSetting("MultiDBAutoStartTime", "0");

        validateSetting("SpoilersDescriptionTags", "");
        validateSetting("AutoResort", "false");

        validateSetting("HtcCompass", "false");
        validateSetting("HtcLevel", "30");
        validateSetting("SmoothScrolling", "none");

        validateSetting("DebugShowPanel", "false");
        validateSetting("DebugMemory", "false");

        validateSetting("LockM", "1");
        validateSetting("LockSec", "0");
        validateSetting("AllowLandscape", "false");
        validateSetting("MoveMapCenterWithSpeed", "false");
        validateSetting("MoveMapCenterMaxSpeed", "20");
        validateSetting("lastZoomLevel", "14");
        validateSetting("quickButtonShow", "true");
        validateSetting("quickButtonList", "1,15,14,19,12,23,2,13");
        validateSetting("PremiumMember", "false");

        // api search settings
        validateSetting("SearchWithoutFounds", "true");
        validateSetting("SearchWithoutOwns", "true");
        validateSetting("SearchOnlyAvailable", "true");

        // validateSetting("OtherRepositoriesFolder", Global.AppPath +
        // "/Repositories");

        AcceptChanges();
    }

    private static void validateSetting(String key, String value) {
        String Lookupvalue = keyLookup.get(key);
        if (Lookupvalue == null)
            keyLookup.put(key, value);
    }

    public static void Set(String key, String value) {
        checkInitialization();
        keyLookup.put(key, value);
    }

    public static String GetStringEncrypted(String key) {
        String s;
        boolean convert = false;
        if (ExistsKey(key + "Enc")) {
            s = GetString(key + "Enc");
            if (s != "") {
                // encrypted Key is found -> remove the old non encrypted
                if (ExistsKey(key)) {
                    keyLookup.remove(key);
                    AcceptChanges();
                }
                s = decrypt(s);
            }
        } else {
            // no encrypted Key is found -> search for non encrypted
            s = GetString(key);
            if (s != "") {
                // remove the old non encrypted and insert a new encrypted
                keyLookup.remove(key);
                convert = true;
            }
        }

        if (convert) {
            SetEncrypted(key, s);
            AcceptChanges();
        }
        return s;
    }

    public static boolean ExistsKey(String key) {
        checkInitialization();
        return keyLookup.containsKey(key);
    }

    public static void SetEncrypted(String key, String value) {
        String encrypted = encrypt(value);
        if (ExistsKey(key))
            keyLookup.remove(key); // remove non decrypted key
        // if exists
        Set(key + "Enc", encrypted);
    }

    public static void AcceptChanges() {
        that.acceptChanges();
    }

    protected void acceptChanges() {
        if (settings.WriteToDB()) {

            //TODO change to Dialog for restart now
            GL.that.Toast(Translation.Get("SettingChangesNeedRestart"));
        }
    }

}
