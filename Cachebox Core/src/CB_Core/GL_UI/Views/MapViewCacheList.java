package CB_Core.GL_UI.Views;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import CB_Core.GlobalCore;
import CB_Core.DB.Database;
import CB_Core.Enums.CacheTypes;
import CB_Core.GL_UI.SpriteCache;
import CB_Core.Log.Logger;
import CB_Core.Map.Descriptor;
import CB_Core.Types.Cache;
import CB_Core.Types.MysterySolution;
import CB_Core.Types.Waypoint;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class MapViewCacheList
{
	private int maxZoomLevel;
	private queueProcessor queueProcessor = null;

	/**
	 * State 0: warten auf neuen Update Befehl <br>
	 * State 1: Berechnen <br>
	 * State 2: Berechnung in Gang <br>
	 * State 3: Berechnung fertig - warten auf abholen <br>
	 * State 4: queueProcessor abgebrochen
	 */
	private AtomicInteger state = new AtomicInteger(0);
	private Vector2 point1;
	private Vector2 point2;
	private int zoom = 15;
	public ArrayList<WaypointRenderInfo> list = new ArrayList<MapViewCacheList.WaypointRenderInfo>();
	public ArrayList<WaypointRenderInfo> tmplist;
	public int anz = 0;
	private boolean hideMyFinds = false;
	private boolean showAllWaypoints = false;

	// public ArrayList<ArrayList<Sprite>> NewMapIcons = null;
	// public ArrayList<ArrayList<Sprite>> NewMapOverlay = null;

	public MapViewCacheList(int maxZoomLevel)
	{
		super();
		this.maxZoomLevel = maxZoomLevel;

		StartQueueProcessor();

	}

	private void StartQueueProcessor()
	{

		try
		{
			Logger.DEBUG("MapCacheList.queueProcessor Create");
			queueProcessor = new queueProcessor();
			queueProcessor.setPriority(Thread.MIN_PRIORITY);
		}
		catch (Exception ex)
		{
			Logger.Error("MapCacheList.queueProcessor", "onCreate", ex);
		}

		Logger.DEBUG("MapCacheList.queueProcessor Start");
		queueProcessor.start();

		state.set(0);
	}

	private class queueProcessor extends Thread
	{

		@Override
		public void run()
		{
			// boolean queueEmpty = false;
			try
			{
				do
				{
					if (state.compareAndSet(1, 2))
					{
						int iconSize = 0; // 8x8
						if ((zoom >= 13) && (zoom <= 14)) iconSize = 1; // 13x13
						else if (zoom > 14) iconSize = 2; // default Images

						tmplist = new ArrayList<MapViewCacheList.WaypointRenderInfo>();

						synchronized (Database.Data.Query)
						{
							for (Cache cache : Database.Data.Query)
							{
								// Funde
								if (hideMyFinds && cache.Found) continue;
								// im Bild ?
								double MapX = 256.0 * Descriptor.LongitudeToTileX(maxZoomLevel, cache.Longitude());
								double MapY = -256.0 * Descriptor.LatitudeToTileY(maxZoomLevel, cache.Latitude());
								boolean CacheIsNotVisible = !((MapX >= point1.x) && (MapX < point2.x)
										&& (Math.abs(MapY) > Math.abs(point1.y)) && (Math.abs(MapY) < Math.abs(point2.y)));
								if (CacheIsNotVisible && !(showAllWaypoints || GlobalCore.SelectedCache() == cache))
								{
									// keine Wegpunkte anzuzeigen und Cache nicht im Bild
									continue;
								}
								// zuerst Wegpunkte hinzuf�gen, damit die Anzeige erfolgt, auch wenn Cache nicht im Bild ist
								if (showAllWaypoints || GlobalCore.SelectedCache() == cache)
								{
									addWaypoints(cache);
								}
								else
								{
									if (cache.Type == CacheTypes.Mystery)
									{
										if (cache.CorrectedCoordiantesOrMysterySolved())
										{
											if (cache.HasFinalWaypoint())
											{
												// k�nnte man eigentlich auch jetzt schon hinzuf�gen, aber
												// Final statt Cache sp�ter hinzuf�gen aus den MysterySolutions
												// (mit ge�ndertem Symbol)
												continue;
											}
										}
									}
									// kein Final, bzw Wegpunkte nicht anzeigen, dann den Cache anzeigen
								}
								// im Bild?
								if (CacheIsNotVisible) continue;

								// Cache zeigen
								WaypointRenderInfo wpi = new WaypointRenderInfo();
								wpi.MapX = (float) MapX;
								wpi.MapY = (float) MapY;
								if (cache.Archived || !cache.Available) wpi.OverlayIcon = SpriteCache.MapOverlay.get(2);
								if ((iconSize < 1) && (cache != GlobalCore.SelectedCache()))
								{
									wpi.Icon = getSmallMapIcon(cache);
									wpi.UnderlayIcon = null;
								}
								else
								{
									// der SelectedCache wird immer mit den gro�en Symbolen dargestellt!
									wpi.Icon = getMapIcon(cache);
									wpi.UnderlayIcon = getUnderlayIcon(cache, wpi.Waypoint);
								}
								wpi.Cache = cache;
								wpi.Waypoint = null;
								wpi.Selected = (GlobalCore.SelectedCache() == cache);

								tmplist.add(wpi);

							}

							if (!showAllWaypoints)
							{
								// Final-Waypoints von Mysteries einzeichnen
								// MysterySolutions enth�lt g�ltige Finals von Mystries, Multis und WhereIGos
								for (MysterySolution solution : Database.Data.Query.MysterySolutions)
								{
									if (GlobalCore.SelectedCache() == solution.Cache) continue;

									if (hideMyFinds && solution.Cache.Found) continue;

									double mapX = 256.0 * Descriptor.LongitudeToTileX(maxZoomLevel, solution.Longitude);
									double mapY = -256.0 * Descriptor.LatitudeToTileY(maxZoomLevel, solution.Latitude);
									if (!((mapX >= point1.x) && (mapX < point2.x) && (Math.abs(mapY) > Math.abs(point1.y)) && (Math
											.abs(mapY) < Math.abs(point2.y)))) continue;

									WaypointRenderInfo wpiF = new WaypointRenderInfo();
									wpiF.MapX = (float) mapX;
									wpiF.MapY = (float) mapY;

									if (iconSize == 2)
									{
										wpiF.Icon = (solution.Cache.Type == CacheTypes.Mystery) ? SpriteCache.MapIcons.get(21)
												: SpriteCache.MapIcons.get(18);
										wpiF.UnderlayIcon = getUnderlayIcon(solution.Cache, solution.Waypoint);
										if ((solution.Cache.Type == CacheTypes.Mystery)
												&& solution.Cache.CorrectedCoordiantesOrMysterySolved()
												&& solution.Cache.HasFinalWaypoint())
										{
											if (GlobalCore.SelectedCache() != solution.Cache)
											{
												// die Icons aller geloesten Mysterys
												// evtl. aendern,
												// wenn der Cache gefunden oder ein
												// Eigener ist.
												// change the icon of solved mysterys if
												// necessary
												// when the cache is found or own
												if (solution.Cache.Found) wpiF.Icon = SpriteCache.MapIcons.get(19);
												if (solution.Cache.ImTheOwner()) wpiF.Icon = SpriteCache.MapIcons.get(22);
											}
											else
											{
												// das Icon des geloesten Mysterys als
												// Final
												// anzeigen, wenn dieser Selected ist
												// show the Icon of solved mysterys as
												// final when
												// cache is selected
												wpiF.Icon = SpriteCache.MapIcons.get((int) solution.Waypoint.Type.ordinal());
											}
										}
									}
									else
									{
										wpiF.Icon = getSmallMapIcon(solution.Cache);
										wpiF.OverlayIcon = null;
									}
									wpiF.Cache = solution.Cache;
									wpiF.Waypoint = null; // solution.Waypoint;
									wpiF.Selected = (GlobalCore.SelectedWaypoint() == solution.Waypoint);
									if (iconSize > 0) wpiF.UnderlayIcon = getUnderlayIcon(solution.Cache, solution.Waypoint);

									tmplist.add(wpiF);
								}
							}
						}

						synchronized (list)
						{
							list.clear();
							list = tmplist;
							tmplist = null;
						}
						Thread.sleep(50);
						state.set(0);
						anz++;
						if (savedQuery != null)
						{
							// es steht noch eine Anfrage an!
							// Diese jetzt ausf�hren!
							MapViewCacheListUpdateData data = new MapViewCacheListUpdateData(savedQuery);
							data.hideMyFinds = MapViewCacheList.this.hideMyFinds;
							data.showAllWaypoints = MapViewCacheList.this.showAllWaypoints;
							savedQuery = null;
							update(data);
						}
					}
					else
					{
						Thread.sleep(50);
					}
				}
				while (true);
			}
			catch (Exception ex3)
			{
				Logger.Error("MapCacheList.queueProcessor.doInBackground()", "3", ex3);
			}
			finally
			{
				// wenn der Thread beendet wurde, muss er neu gestartet werden!
				state.set(4);
			}
			return;
		}
	}

	private void addWaypoints(Cache cache)
	{
		for (Waypoint wp : cache.waypoints)
		{
			// im Bild?
			double MapX = 256.0 * Descriptor.LongitudeToTileX(maxZoomLevel, wp.Pos.getLongitude());
			double MapY = -256.0 * Descriptor.LatitudeToTileY(maxZoomLevel, wp.Pos.getLatitude());
			if ((MapX >= point1.x) && (MapX < point2.x) && (Math.abs(MapY) > Math.abs(point1.y)) && (Math.abs(MapY) < Math.abs(point2.y)))
			{
				WaypointRenderInfo wpi = new WaypointRenderInfo();
				wpi.MapX = (float) MapX;
				wpi.MapY = (float) MapY;
				wpi.Icon = SpriteCache.MapIcons.get(wp.Type.ordinal());
				wpi.Cache = cache;
				wpi.Waypoint = wp;
				wpi.Selected = (GlobalCore.SelectedWaypoint() == wp);
				wpi.UnderlayIcon = getUnderlayIcon(wpi.Cache, wpi.Waypoint);

				tmplist.add(wpi);
			}
		}
	}

	private Sprite getMapIcon(Cache cache)
	{
		int IconId;
		if (cache.ImTheOwner()) IconId = 22;
		else if (cache.Found) IconId = 19;
		else if ((cache.Type == CacheTypes.Mystery) && cache.CorrectedCoordiantesOrMysterySolved()) IconId = 21;
		else
			IconId = cache.Type.ordinal();
		return SpriteCache.MapIcons.get(IconId);
	}

	private Sprite getSmallMapIcon(Cache cache)
	{
		int iconId = 0;

		switch (cache.Type)
		{
		case Traditional:
			iconId = 0;
			break;
		case Letterbox:
			iconId = 0;
			break;
		case Multi:
			iconId = 1;
			break;
		case Event:
			iconId = 2;
			break;
		case MegaEvent:
			iconId = 2;
			break;
		case Virtual:
			iconId = 3;
			break;
		case Camera:
			iconId = 3;
			break;
		case Earth:
			iconId = 3;
			break;
		case Mystery:
		{
			if (cache.HasFinalWaypoint()) iconId = 5;
			else
				iconId = 4;
			break;
		}
		case Wherigo:
			iconId = 4;
			break;
		default:
			iconId = 0;
		}

		if (cache.Found) iconId = 6;
		if (cache.ImTheOwner()) iconId = 7;

		if (cache.Archived || !cache.Available) iconId += 8;

		if (cache.Type == CacheTypes.MyParking) iconId = 16;

		return SpriteCache.MapIconsSmall.get(iconId);

	}

	private Sprite getUnderlayIcon(Cache cache, Waypoint waypoint)
	{
		if (waypoint == null)
		{
			if ((cache == null) || (cache == GlobalCore.SelectedCache()))
			{
				return SpriteCache.MapOverlay.get(1);
			}
			else
			{
				return SpriteCache.MapOverlay.get(0);
			}
		}
		else
		{
			if (waypoint == GlobalCore.SelectedWaypoint())
			{
				return SpriteCache.MapOverlay.get(1);
			}
			else
			{
				return SpriteCache.MapOverlay.get(0);
			}
		}
	}

	private Vector2 lastPoint1;
	private Vector2 lastPoint2;
	private int lastzoom;

	public static class MapViewCacheListUpdateData
	{
		public Vector2 point1;
		public Vector2 point2;
		public int zoom;
		public boolean doNotCheck;
		public boolean hideMyFinds = false;
		public boolean showAllWaypoints = false;

		public MapViewCacheListUpdateData(Vector2 point1, Vector2 point2, int zoom, boolean doNotCheck)
		{
			this.point1 = point1;
			this.point2 = point2;
			this.zoom = zoom;
			this.doNotCheck = doNotCheck;
		}

		public MapViewCacheListUpdateData(MapViewCacheListUpdateData data)
		{
			this.point1 = data.point1;
			this.point2 = data.point2;
			this.zoom = data.zoom;
			this.doNotCheck = data.doNotCheck;
		}
	}

	MapViewCacheListUpdateData savedQuery = null;

	public void update(MapViewCacheListUpdateData data)
	{

		this.showAllWaypoints = data.showAllWaypoints;
		this.hideMyFinds = data.hideMyFinds;

		if (data.point1 == null || data.point2 == null) return;

		if (state.get() == 4)
		{
			// der queueProcessor wurde gestoppt und muss neu gestartet werden
			StartQueueProcessor();
		}

		if (state.get() != 0)
		{
			// Speichere Update anfrage und f�hre sie aus, wenn der queueProcessor wieder bereit ist!
			savedQuery = data;
			return;
		}

		if ((data.zoom == lastzoom) && (!data.doNotCheck))
		{
			// wenn LastPoint == 0 muss eine neue Liste Berechnet werden!
			if (lastPoint1 != null && lastPoint2 != null)
			{
				// Pr�fen, ob �berhaupt eine neue Liste berechnet werden mu�
				if ((data.point1.x >= lastPoint1.x) && (data.point2.x <= lastPoint2.x) && (data.point1.y >= lastPoint1.y)
						&& (data.point2.y <= lastPoint2.y)) return;
			}

		}

		// Bereich erweitern, damit von vorne herein gleiche mehr Caches geladen werden und diese Liste nicht so oft berechnet werden muss
		Vector2 size = new Vector2(data.point2.x - data.point1.x, data.point2.y - data.point1.y);
		data.point1.x -= size.x;
		data.point2.x += size.x;
		data.point1.y -= size.y;
		data.point2.y += size.y;

		this.lastzoom = data.zoom;
		lastPoint1 = data.point1;
		lastPoint2 = data.point2;

		this.zoom = data.zoom;
		this.point1 = data.point1;
		this.point2 = data.point2;
		state.set(1);
	}

	public boolean hasNewResult()
	{
		return state.get() == 3;
	}

	public static class WaypointRenderInfo
	{
		public float MapX;
		public float MapY;
		public Cache Cache;
		public Waypoint Waypoint;
		public boolean Selected;
		public Sprite Icon;
		public Sprite UnderlayIcon;
		public Sprite OverlayIcon;
	};

}
