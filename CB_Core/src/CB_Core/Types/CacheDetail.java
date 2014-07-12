package CB_Core.Types;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import CB_Core.DB.Database;
import CB_Core.Enums.Attributes;
import CB_Core.Settings.CB_Core_Settings;
import CB_Utils.DB.CoreCursor;
import CB_Utils.Lists.CB_List;
import CB_Utils.Util.FileIO;

public class CacheDetail implements Serializable
{
	private static final long serialVersionUID = 2088367633865443637L;

	/*
	 * Public Member
	 */

	/**
	 * Id des Caches bei geocaching.com. Wird zumm Loggen benoetigt und von geotoad nicht exportiert
	 */
	// TODO Warum ist das ein String?
	private byte[] GcId;

	/**
	 * Erschaffer des Caches
	 */
	public String PlacedBy = "";

	/**
	 * Datum, an dem der Cache versteckt wurde
	 */
	public Date DateHidden;

	/**
	 * ApiStatus 0: Cache wurde nicht per Api hinzugefuegt 1: Cache wurde per GC Api hinzugefuegt und ist noch nicht komplett geladen
	 * (IsLite = true) 2: Cache wurde per GC Api hinzugefuegt und ist komplett geladen (IsLite = false)
	 */
	public byte ApiStatus;

	/**
	 * for Replication
	 */
	public int noteCheckSum = 0;
	public String tmpNote = null; // nur fuer den RPC-Import

	/**
	 * for Replication
	 */
	public int solverCheckSum = 0;
	public String tmpSolver = null; // nur fuer den RPC-Import

	/**
	 * Name der Tour, wenn die GPX-Datei aus GCTour importiert wurde
	 */
	public String TourName = "";

	/**
	 * Name der GPX-Datei aus der importiert wurde
	 */
	public long GPXFilename_ID = 0;

	/**
	 * URL des Caches
	 */
	public String Url = "";

	/**
	 * Country des Caches
	 */
	public String Country = "";

	/**
	 * State des Caches
	 */
	public String State = "";

	/**
	 * Positive Attribute des Caches
	 */
	private DLong attributesPositive = new DLong(0, 0);

	/**
	 * Negative Attribute des Caches
	 */
	private DLong attributesNegative = new DLong(0, 0);

	/**
	 * Hinweis fuer diesen Cache
	 */
	private String hint = "";

	/**
	 * Liste der Spoiler Resorcen
	 */
	public CB_List<ImageEntry> spoilerRessources = null;

	/**
	 * Kurz Beschreibung des Caches
	 */
	public String shortDescription;

	/**
	 * Ausfuehrliche Beschreibung des Caches Nur fuer Import Zwecke. Ist normalerweise leer, da die Description bei aus Speicherplatz
	 * Gruenden bei Bedarf aus der DB geladen wird
	 */
	public String longDescription;

	/*
	 * Constructors
	 */

	/**
	 * Constructor
	 */
	public CacheDetail()
	{
		this.DateHidden = new Date();
		AttributeList = null;

	}

	public void dispose()
	{
		// clear all Lists
		if (AttributeList != null)
		{
			AttributeList.clear();
			AttributeList = null;
		}

		if (spoilerRessources != null)
		{
			for (int i = 0, n = spoilerRessources.size(); i < n; i++)
			{
				ImageEntry entry = spoilerRessources.get(i);
				entry.dispose();
			}
			spoilerRessources.clear();
			spoilerRessources = null;
		}

		// if (waypoints != null)
		// {
		// for (int i = 0, n = waypoints.size(); i < n; i++)
		// {
		// Waypoint entry = waypoints.get(i);
		// entry.dispose();
		// }
		//
		// waypoints.clear();
		// }

		tmpNote = null;
		tmpSolver = null;
		TourName = null;
		PlacedBy = null;
		// setOwner(null);
		DateHidden = null;
		Url = null;
		Country = null;
		State = null;
		// setHint(null);
		shortDescription = null;
		longDescription = null;

	}

	public String getGcId()
	{
		if (GcId == null) return Cache.EMPTY_STRING;
		return new String(GcId, Cache.US_ASCII);
	}

	public void setGcId(String gcId)
	{
		if (gcId == null)
		{
			GcId = null;
			return;
		}
		GcId = gcId.getBytes(Cache.US_ASCII);
	}

	public boolean isAttributePositiveSet(Attributes attribute)
	{
		return attributesPositive.BitAndBiggerNull(Attributes.GetAttributeDlong(attribute));
		// return (attributesPositive & Attributes.GetAttributeDlong(attribute))
		// > 0;
	}

	public boolean isAttributeNegativeSet(Attributes attribute)
	{
		return attributesNegative.BitAndBiggerNull(Attributes.GetAttributeDlong(attribute));
		// return (attributesNegative & Attributes.GetAttributeDlong(attribute))
		// > 0;
	}

	public void addAttributeNegative(Attributes attribute)
	{
		if (attributesNegative == null) attributesNegative = new DLong(0, 0);
		attributesNegative.BitOr(Attributes.GetAttributeDlong(attribute));
	}

	public void addAttributePositive(Attributes attribute)
	{
		if (attributesPositive == null) attributesPositive = new DLong(0, 0);
		attributesPositive.BitOr(Attributes.GetAttributeDlong(attribute));
	}

	public void setAttributesPositive(DLong i)
	{
		attributesPositive = i;
	}

	public void setAttributesNegative(DLong i)
	{
		attributesNegative = i;
	}

	public DLong getAttributesNegative(long Id)
	{
		if (this.attributesNegative == null)
		{
			CoreCursor c = Database.Data.rawQuery("select AttributesNegative,AttributesNegativeHigh from Caches where Id=?", new String[]
				{ String.valueOf(Id) });
			c.moveToFirst();
			while (c.isAfterLast() == false)
			{
				if (!c.isNull(0)) this.attributesNegative = new DLong(c.getLong(1), c.getLong(0));
				else
					this.attributesNegative = new DLong(0, 0);
				break;
			}
			;
			c.close();
		}
		return this.attributesNegative;
	}

	public DLong getAttributesPositive(long Id)
	{
		if (this.attributesPositive == null)
		{
			CoreCursor c = Database.Data.rawQuery("select AttributesPositive,AttributesPositiveHigh from Caches where Id=?", new String[]
				{ String.valueOf(Id) });
			c.moveToFirst();
			while (c.isAfterLast() == false)
			{
				if (!c.isNull(0)) this.attributesPositive = new DLong(c.getLong(1), c.getLong(0));
				else
					this.attributesPositive = new DLong(0, 0);
				break;
			}
			;
			c.close();
		}
		return this.attributesPositive;
	}

	private ArrayList<Attributes> AttributeList = null;

	public ArrayList<Attributes> getAttributes(long Id)
	{
		if (AttributeList == null)
		{
			AttributeList = Attributes.getAttributes(this.getAttributesPositive(Id), this.getAttributesNegative(Id));
		}

		return AttributeList;
	}

	public String getHint()
	{
		return hint;
	}

	public void setHint(String hint2)
	{
		this.hint = hint2;

	}

	/**
	 * Returns a List of Spoiler Ressources
	 * 
	 * @return ArrayList of String
	 */
	public CB_List<ImageEntry> getSpoilerRessources(Cache cache)
	{
		if (spoilerRessources == null)
		{
			ReloadSpoilerRessources(cache);
		}

		return spoilerRessources;
	}

	/**
	 * Set a List of Spoiler Ressources
	 * 
	 * @param value
	 *            ArrayList of String
	 */
	public void setSpoilerRessources(CB_List<ImageEntry> value)
	{
		spoilerRessources = value;
	}

	/**
	 * Returns true has the Cache Spoilers else returns false
	 * 
	 * @return Boolean
	 */
	public boolean SpoilerExists(Cache cache)
	{
		try
		{
			if (spoilerRessources == null) ReloadSpoilerRessources(cache);
			return spoilerRessources.size() > 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * @param SpoilerFolderLocal
	 *            Config.settings.SpoilerFolderLocal.getValue()
	 * @param DefaultSpoilerFolder
	 *            Config.settings.SpoilerFolder.getDefaultValue()
	 * @param DescriptionImageFolder
	 *            Config.settings.DescriptionImageFolder.getValue()
	 * @param UserImageFolder
	 *            Config.settings.UserImageFolder.getValue()
	 */
	public void ReloadSpoilerRessources(Cache cache)
	{
		spoilerRessources = new CB_List<ImageEntry>();

		String directory = "";

		// from own Repository
		String path = CB_Core_Settings.SpoilerFolderLocal.getValue();
		if (path != null && path.length() > 0)
		{
			directory = path + "/" + cache.getGcCode().substring(0, 4);
			reloadSpoilerResourcesFromPath(directory, spoilerRessources, cache);
		}

		// from Global Repository
		path = CB_Core_Settings.DescriptionImageFolder.getValue();
		directory = path + "/" + cache.getGcCode().substring(0, 4);
		reloadSpoilerResourcesFromPath(directory, spoilerRessources, cache);

		// Spoilers are always loaden from global Repository too
		// from globalUser changed Repository
		path = CB_Core_Settings.SpoilerFolder.getValue();
		directory = path + "/" + cache.getGcCode().substring(0, 4);
		reloadSpoilerResourcesFromPath(directory, spoilerRessources, cache);

		// Add own taken photo
		directory = CB_Core_Settings.UserImageFolder.getValue();
		if (directory != null)
		{
			reloadSpoilerResourcesFromPath(directory, spoilerRessources, cache);
		}
	}

	private void reloadSpoilerResourcesFromPath(String directory, CB_List<ImageEntry> spoilerRessources2, final Cache cache)
	{
		if (!FileIO.DirectoryExists(directory)) return;
		// Logger.DEBUG("Loading spoilers from " + directory);
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String filename)
			{
				filename = filename.toLowerCase(Locale.getDefault());
				if (filename.indexOf(cache.getGcCode().toLowerCase(Locale.getDefault())) >= 0)
				{
					if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp") || filename.endsWith(".png")
							|| filename.endsWith(".gif")) return true;
				}
				return false;
			}
		};
		String[] files = dir.list(filter);
		if (!(files == null))
		{
			if (files.length > 0)
			{
				for (String file : files)
				{
					String ext = FileIO.GetFileExtension(file);
					if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("bmp")
							|| ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("gif"))
					{
						ImageEntry imageEntry = new ImageEntry();
						imageEntry.LocalPath = directory + "/" + file;
						imageEntry.Name = file;
						spoilerRessources.add(imageEntry);
					}
				}
			}
		}
	}

	public void setLongDescription(String value)
	{
		longDescription = value;
	}

	public String getLongDescription()
	{
		return longDescription;
	}

	public void setShortDescription(String value)
	{
		shortDescription = value;
	}

	public String getShortDescription()
	{
		return shortDescription;
	}

}
