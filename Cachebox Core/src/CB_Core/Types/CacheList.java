package CB_Core.Types;

import java.util.Collections;

import CB_Core.GlobalCore;
import CB_Core.Enums.CacheTypes;

public class CacheList extends MoveableList<Cache>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Cache GetCacheByGcCode(String GcCode)
	{
		for (Cache cache : this)
		{
			if (cache.GcCode.equalsIgnoreCase(GcCode)) return cache;
		}
		return null;
	}

	public Cache GetCacheById(long cacheId)
	{
		for (Cache cache : this)
		{
			if (cache.Id == cacheId) return cache;
		}
		return null;
	}

	public void Resort()
	{
		if (GlobalCore.LastValidPosition == null) return;

		GlobalCore.ResortAtWork = true;
		// Alle Distanzen aktualisieren
		for (Cache cache : this)
		{
			cache.Distance(true);
		}

		Collections.sort(this);

		// N�chsten Cache ausw�hlen
		if (this.size() > 0)
		{
			Cache nextCache = this.get(0); // or null ...
			for (int i = 0; i < this.size(); i++)
			{
				nextCache = this.get(i);
				if (!nextCache.Archived)
				{
					if (nextCache.Available)
					{
						if (!nextCache.Found) // eigentlich wenn has_fieldnote(found,DNF,Maint,SBA, aber note vielleicht nicht) , aber found
												// kann nicht r�ckg�ngig gemacht werden.
						{
							if (!nextCache.ImTheOwner())
							{
								if (nextCache.Type != CacheTypes.Mystery)
								{
									break;
								}
								else
								{
									if (nextCache.CorrectedCoordiantesOrMysterySolved())
									{
										break;
									}
								}
							}
						}
					}
				}
			}
			// Wenn der nachste Cache ein Mystery mit Final Waypoint ist
			// -> gleich den Final Waypoint auswahlen!!!
			// When the next Cache is a mystery with final waypoint
			// -> activate the final waypoint!!!
			Waypoint waypoint = nextCache.GetFinalWaypoint();

			// do not Change AutoResort Flag when selecting a Cache in the Resort function
			GlobalCore.setSelectedWaypoint(nextCache, waypoint, false);
			GlobalCore.NearestCache(nextCache);
		}

		CB_Core.Events.CachListChangedEventList.Call();

		// vorhandenen Parkplatz Cache nach oben schieben
		Cache park = this.GetCacheByGcCode("CBPark");
		if (park != null)
		{
			this.MoveItemFirst(this.indexOf(park));
		}

		// Cursor.Current = Cursors.Default;
		GlobalCore.ResortAtWork = false;
	}

	public void checkSelectedCacheValid()
	{
		// Pr�fen, ob der SelectedCache noch in der cacheList drin ist.
		if ((size() > 0) && (GlobalCore.getSelectedCache() != null) && (GetCacheById(GlobalCore.getSelectedCache().Id) == null))
		{
			// der SelectedCache ist nicht mehr in der cacheList drin -> einen beliebigen aus der CacheList ausw�hlen
			GlobalCore.setSelectedCache(get(0));
		}
		// Wenn noch kein Cache Selected ist dann einfach den ersten der Liste aktivieren
		if ((GlobalCore.getSelectedCache() == null) && (size() > 0))
		{
			GlobalCore.setSelectedCache(get(0));
		}
	}

}
