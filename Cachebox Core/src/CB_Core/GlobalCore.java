package CB_Core;

import CB_Core.Enums.SmoothScrollingTyp;
import CB_Core.Events.SelectedCacheEventList;
import CB_Core.Events.platformConector;
import CB_Core.Log.Logger;
import CB_Core.Map.RouteOverlay;
import CB_Core.TranslationEngine.LangStrings;
import CB_Core.Types.Cache;
import CB_Core.Types.Coordinate;
import CB_Core.Types.Waypoint;

import com.badlogic.gdx.scenes.scene2d.ui.utils.Clipboard;

public class GlobalCore
{

	public static final int CurrentRevision = 883;
	public static final String CurrentVersion = "0.5.";
	public static final String VersionPrefix = "Test";

	public static final String br = System.getProperty("line.separator");
	public static final String splashMsg = "Team Cachebox (2011-2012)" + br + "www.team-cachebox.de" + br + "Cache Icons Copyright 2009,"
			+ br + "Groundspeak Inc. Used with permission" + br + br + br + "POWERED BY:";

	// / <summary>
	// / Letzte bekannte Position
	// / </summary>
	public static Coordinate LastValidPosition = new Coordinate();
	public static Coordinate LastPosition = new Coordinate();
	public static Coordinate Marker = new Coordinate();
	public static boolean ResortAtWork = false;
	public static final int LatestDatabaseChange = 1021;
	public static final int LatestDatabaseFieldNoteChange = 1001;
	public static final int LatestDatabaseSettingsChange = 1002;
	public static double displayDensity = 1;
	public static Plattform platform = Plattform.undef;

	public static CB_Core.Types.Locator Locator = null;

	public static RouteOverlay.Trackable AktuelleRoute = null;
	public static int aktuelleRouteCount = 0;
	public static long TrackDistance;

	private static Clipboard defaultClipBoard;

	public static Clipboard getDefaultClipboard()
	{
		if (defaultClipBoard == null)
		{
			return Clipboard.getDefaultClipboard();
		}
		else
		{
			return defaultClipBoard;
		}
	}

	public static void setDefaultClipboard(Clipboard clipBoard)
	{
		defaultClipBoard = clipBoard;
	}

	/**
	 * Wird im Splash gesetzt und ist True, wenn es sich um ein Tablet handelt!
	 */
	public static boolean isTab = false;

	public static LangStrings Translations = new LangStrings();

	private static Cache selectedCache = null;
	public static boolean autoResort;

	public static SmoothScrollingTyp SmoothScrolling = SmoothScrollingTyp.normal;

	public static FilterProperties LastFilter = null;

	public static void SelectedCache(Cache cache)
	{
		selectedCache = cache;
		GlobalCore.selectedWaypoint = null;
		SelectedCacheEventList.Call(cache, null);

		// switch off auto select
		GlobalCore.autoResort = false;
		Config.settings.AutoResort.setValue(GlobalCore.autoResort);

	}

	public static Cache SelectedCache()
	{
		return selectedCache;
	}

	private static Cache nearestCache = null;

	public static Cache NearestCache()
	{
		return nearestCache;
	}

	private static Waypoint selectedWaypoint = null;

	public static void SelectedWaypoint(Cache cache, Waypoint waypoint)
	{
		selectedCache = cache;
		selectedWaypoint = waypoint;
		SelectedCacheEventList.Call(selectedCache, waypoint);
	}

	public static void NearestCache(Cache nearest)
	{
		nearestCache = nearest;
	}

	public static Waypoint SelectedWaypoint()
	{
		return selectedWaypoint;
	}

	public static CB_Core.Types.Categories Categories = null;

	// / <summary>
	// / SDBM-Hash algorithm for storing hash values into the database. This is
	// neccessary to be compatible to the CacheBox@Home project. Because the
	// / standard .net Hash algorithm differs from compact edition to the normal
	// edition.
	// / </summary>
	// / <param name="str"></param>
	// / <returns></returns>
	public static long sdbm(String str)
	{
		if (str == null || str.equals("")) return 0;

		long hash = 0;
		// set mask to 2^32!!!???!!!
		long mask = 42949672;
		mask = mask * 100 + 95;

		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			hash = (c + (hash << 6) + (hash << 16) - hash) & mask;
		}

		return hash;
	}

	static String FormatDM(double coord, String positiveDirection, String negativeDirection)
	{
		int deg = (int) coord;
		double frac = coord - deg;
		double min = frac * 60;

		String result = Math.abs(deg) + "\u00B0  " + String.format("%.3f", Math.abs(min));

		result += " ";

		if (coord < 0) result += negativeDirection;
		else
			result += positiveDirection;

		return result;
	}

	public static String FormatLatitudeDM(double latitude)
	{
		return FormatDM(latitude, "N", "S");
	}

	public static String FormatLongitudeDM(double longitude)
	{
		return FormatDM(longitude, "E", "W");
	}

	public static String Rot13(String message)
	{
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String lookup = "nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM";

		String result = "";

		for (int i = 0; i < message.length(); i++)
		{
			String curChar = message.substring(i, i + 1);
			int idx = alphabet.indexOf(curChar);

			if (idx < 0) result += curChar;
			else
				result += lookup.substring(idx, idx + 1);
		}

		return result;

	}

	/**
	 * APIisOnline Liefert TRUE wenn die M�glichkeit besteht auf das Internet zuzugreifen und ein API Access Token vorhanden ist.
	 */
	public static boolean APIisOnline()
	{
		if (Config.GetAccessToken().length() == 0)
		{
			Logger.General("global.APIisOnline() -Invalid AccessToken");
			return false;
		}
		if (platformConector.isOnline())
		{
			return true;
		}
		return false;
	}

	/**
	 * JokerisOnline Liefert TRUE wenn die M�glichkeit besteht auf das Internet zuzugreifen und ein Passwort f�r gcJoker.de vorhanden ist.
	 */
	public static boolean JokerisOnline()
	{
		if (Config.settings.GcJoker.getValue().length() == 0)
		{
			Logger.General("global.APIisOnline() -Invalid Joker");
			return false;
		}
		if (platformConector.isOnline())
		{
			return true;
		}
		return false;
	}

	public static String getVersionString()
	{
		final String ret = "Version: " + CurrentVersion + String.valueOf(CurrentRevision) + "  "
				+ (VersionPrefix.equals("") ? "" : "(" + VersionPrefix + ")");
		return ret;
	}

}
