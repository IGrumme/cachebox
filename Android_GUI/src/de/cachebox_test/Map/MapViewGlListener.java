package de.cachebox_test.Map;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.DestroyFailedException;

import CB_Core.Config;
import CB_Core.GlobalCore;
import CB_Core.DB.Database;
import CB_Core.Log.Logger;
import CB_Core.Map.Descriptor;
import CB_Core.Map.Descriptor.PointD;
import CB_Core.Types.Cache;
import CB_Core.Types.Coordinate;
import CB_Core.Types.Waypoint;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

import de.cachebox_test.Global;
import de.cachebox_test.UnitFormatter;
import de.cachebox_test.main;
import de.cachebox_test.Components.CacheDraw;
import de.cachebox_test.Custom_Controls.GL_ZoomBtn;
import de.cachebox_test.Custom_Controls.GL_ZoomScale;
import de.cachebox_test.Custom_Controls.MultiToggleButton;
import de.cachebox_test.Events.PositionEvent;
import de.cachebox_test.Events.PositionEventList;
import de.cachebox_test.Map.MapCacheList.WaypointRenderInfo;
import de.cachebox_test.Ui.Sizes;
import de.cachebox_test.Ui.Math.SizeF;
import de.cachebox_test.Views.MapView;
import de.cachebox_test.Views.Forms.ScreenLock;

public class MapViewGlListener implements ApplicationListener, PositionEvent
{
	protected SortedMap<Long, TileGL> loadedTiles = new TreeMap<Long, TileGL>();
	final Lock loadedTilesLock = new ReentrantLock();
	protected SortedMap<Long, Descriptor> queuedTiles = new TreeMap<Long, Descriptor>();
	private Lock queuedTilesLock = new ReentrantLock();
	private queueProcessor queueProcessor = null;
	private AtomicBoolean started = new AtomicBoolean(false);
	public boolean alignToCompass = false;
	// private boolean centerGps = false;
	private float mapHeading = 0;
	private float arrowHeading = 0;
	private MapCacheList mapCacheList;
	private Point lastMovement = new Point(0, 0);
	private int zoomCross = 16;
	private MultiToggleButton btnTrackPos;
	private GL_ZoomBtn zoomBtn;
	private GL_ZoomScale zoomScale;

	// Settings values
	public boolean showRating;
	public boolean showDT;
	public boolean showTitles;
	public boolean hideMyFinds;
	private boolean showCompass;
	public boolean showDirektLine;
	private boolean nightMode;
	private int aktZoom;
	private float startCameraZoom;
	private float endCameraZoom;
	private float diffCameraZoom;

	// #################################################################
	//
	// Min, Max und Act Zoom Werte sind jetzt im "zoomBtn" gespeichert!
	//
	// maxzoom wird 1:1 dargestellt
	// int minzoom = 6;
	// int maxzoom = 20;
	// int zoom = 13;
	// #################################################################

	final int maxMapZoom = 20;

	int maxNumTiles = 100;
	float iconFactor = 1.5f;

	long posx = 8745;
	long posy = 5685;

	// screencenter in World Coordinates (Pixels in Zoom Level maxzoom
	Vector2 screenCenterW = new Vector2(0, 0);
	int width;
	int height;
	int drawingWidth;
	int drawingHeight;

	long pos20y = 363904;
	long size20 = 256;

	public Coordinate center = new Coordinate(48.0, 12.0);
	private boolean positionInitialized = false;
	String CurrentLayer = "germany-0.2.4.map";

	public static SpriteBatch batch;
	Matrix4 textMatrix;

	CharSequence str = "Hello World!";
	OrthographicCamera camera;
	CameraController controller;
	GestureDetector gestureDetector;
	Gdx2DPixmap circle;
	Texture tcircle;
	long startTime;

	public MapViewGlListener()
	{
		super();
		if (queueProcessor == null)
		{
			queueProcessor = new queueProcessor();
			queueProcessor.execute(0);
		}

		// initial Zoom Buttons
		zoomBtn = new GL_ZoomBtn(6, 20, 13);

		// initial Zoom Scale
		zoomScale = new GL_ZoomScale(6, 20, 13);

		mapCacheList = new MapCacheList(maxMapZoom);

	}

	@Override
	public void create()
	{
		PositionEventList.Add(this);

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		drawingWidth = width;
		drawingHeight = height;
		batch = new SpriteBatch();
		camera = new OrthographicCamera(width, height);

		controller = new CameraController();
		gestureDetector = new GestureDetector(20, 0.5f, 2, 0.15f, controller);
		Gdx.input.setInputProcessor(gestureDetector);

		iconFactor = (float) Config.settings.MapViewDPIFaktor.getValue();

		// setScreenCenter(new Descriptor((int) posx, (int) posy, 14));
		// setCenter(new Coordinate(48.0, 12.0));
		textMatrix = new Matrix4().setToOrtho2D(0, 0, width, height);

		circle = new Gdx2DPixmap(16, 16, Gdx2DPixmap.GDX2D_FORMAT_RGB565);
		circle.clear(Color.TRANSPARENT);
		// circle.fillRect(0, 0, 16, 16, Color.YELLOW);
		circle.drawCircle(8, 8, 8, Color.BLACK);

		tcircle = new Texture(new Pixmap(circle));
		aktZoom = zoomBtn.getZoom();
		zoomScale.setZoom(aktZoom);
		camera.zoom = getMapTilePosFactor(aktZoom);
		endCameraZoom = camera.zoom;
		diffCameraZoom = 0;
		camera.position.set((float) screenCenterW.x, (float) screenCenterW.y, 0);
		startTime = System.currentTimeMillis();

		// initial Toggle Button
		btnTrackPos = new MultiToggleButton();
		btnTrackPos.clearStates();
		btnTrackPos.addState("Free", Color.GRAY);
		btnTrackPos.addState("GPS", Color.GREEN);
		btnTrackPos.addState("Lock", Color.RED);
		btnTrackPos.addState("Car", Color.YELLOW);
		btnTrackPos.setState(0, true);

		Sizes.GL.initial();

	}

	@Override
	public void resize(int width, int height)
	{
		// //Log.d("MAPVIEW", "resize" + width + "/" + height);
		textMatrix.setToOrtho2D(0, 0, width, height);
		((main) main.mainActivity).iniInput();// Gr��e des Input bereichs muss neu Berechnet werden
		Sizes.GL.initial(width, height);
	}

	protected SortedMap<Long, TileGL> tilesToDraw = new TreeMap<Long, TileGL>();

	private int forceCounter = 0;
	private int count = 0;
	private boolean forceRender = true;

	/**
	 * Setzt ein Flag, sodass der n�chste Render Durchgang auf jeden Fall abgearbeitet wird.
	 */
	private void forceRender()
	{
		forceRender = true;
		forceCounter = 0;
		count = 0;
	}

	/**
	 * Wertet ein eventuell gesetztes forceRender aus.
	 * 
	 * @return TRUE wenn gerendert werden soll FALSE wenn der Render Vorgang abgebrochen werden kann und stattdessen die Buffer Texture
	 *         gezeichnet werden soll.
	 */
	private boolean renderForced()
	{
		count++;
		if (forceRender)
		{
			forceCounter++;
			// immer mindestens 10 render Durchg�nge durchf�hren,
			// wenn ein Force gesetzt wurde.
			if (forceCounter < 10)
			{
				return true;
			}
			else
			{
				forceCounter = 0;
				forceRender = false;
				return false;
			}
		}

		// alle 50 calls zweimal rendern
		if (count > 1 && count < 100)
		{
			return false;
		}

		if (count > 2) count = 0;
		return true;
	}

	@Override
	public void render()
	{

		reduceFPS();

		if (camera.zoom != endCameraZoom)
		{
			// Zoom Animation
			boolean positive = true;
			float newValue;
			if (camera.zoom < endCameraZoom)
			{
				positive = false;
				newValue = camera.zoom + diffCameraZoom;
				if (newValue > endCameraZoom)// endCameraZoom erreicht?
				{
					camera.zoom = endCameraZoom;
				}
				else
				{
					camera.zoom = newValue;
				}
			}
			if (camera.zoom > endCameraZoom)
			{
				newValue = camera.zoom - diffCameraZoom;
				if (newValue < endCameraZoom)// endCameraZoom erreicht?
				{
					camera.zoom = endCameraZoom;
				}
				else
				{
					camera.zoom = newValue;
				}
			}
			int zoom = maxMapZoom;
			float tmpZoom = camera.zoom;
			float faktor = 1.5f;
			faktor = faktor - iconFactor + 1;
			while (tmpZoom > faktor)
			{
				tmpZoom /= 2;
				zoom--;
			}
			aktZoom = zoom;

			zoomScale.setDiffCameraZoom(1 - (tmpZoom * 2), positive);

		}

		if (SpriteCache.MapIcons == null)
		{
			SpriteCache.LoadSprites();
		}

		if (!started.get()) return;

		loadTiles();

		// if (!renderForced())
		// {
		// if (screenCapture != null)
		// {
		// float width = screenCapture.getTexture().getWidth();
		// float height = screenCapture.getTexture().getHeight();
		// batch.begin();
		// batch.draw(screenCapture, 0, 0, width, height);
		// batch.end();
		//
		// return;
		// }
		// }
		// else
		// {
		// destroyScreenCapture();
		// }

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
		controller.update();

		if (alignToCompass)
		{
			camera.up.x = 0;
			camera.up.y = 1;
			camera.up.z = 0;
			camera.rotate(-mapHeading, 0, 0, 1);
		}
		else
		{
			camera.up.x = 0;
			camera.up.y = 1;
			camera.up.z = 0;
		}

		camera.update();

		renderMapTiles();
		renderOverlay();
		renderUI();

		Gdx.gl.glFlush();
		Gdx.gl.glFinish();
		//
		// createOrUpdateScreenCapture();
		// bindTexture(screenCapture.getTexture());

	}

	private void reduceFPS()
	{
		long endTime = System.currentTimeMillis();
		long dt = endTime - startTime;
		if (dt < 33)
		{
			try
			{
				if (20 - dt > 0) Thread.sleep(20 - dt);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		startTime = System.currentTimeMillis();
	}

	private void renderOverlay()
	{
		batch.setProjectionMatrix(textMatrix);
		batch.begin();

		// calculate icon size
		int iconSize = 0; // 8x8
		if ((aktZoom >= 13) && (aktZoom <= 14)) iconSize = 1; // 13x13
		else if (aktZoom > 14) iconSize = 2; // default Images

		renderWPs(Sizes.GL.WPSizes[iconSize], Sizes.GL.UnderlaySizes[iconSize]);
		renderPositionMarker();
		Bubble.render(Sizes.GL.WPSizes[iconSize]);

		batch.end();
	}

	private void renderUI()
	{
		batch.setProjectionMatrix(textMatrix);
		batch.begin();
		renderInfoPanel();

		btnTrackPos.Render(batch, Sizes.GL.Toggle, Sizes.GL.fontAB22);

		zoomBtn.Render(batch, Sizes.GL.ZoomBtn);
		zoomScale.Render(batch, Sizes.GL.ZoomScale);

		renderDebugInfo();
		batch.end();
	}

	private void renderMapTiles()
	{
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		try
		{
			// das Alter aller Tiles um 1 erh�hen
			for (TileGL tile : loadedTiles.values())
			{
				tile.Age++;
			}
		}
		catch (Exception e)
		{
			// LogCat announces a java.util.ConcurrentModificationException
		}
		// for (int tmpzoom = zoom; tmpzoom <= zoom; tmpzoom++)
		{
			int tmpzoom = aktZoom;
			Descriptor lo = screenToDescriptor(new Vector2(width / 2 - drawingWidth / 2, height / 2 - drawingHeight / 2), tmpzoom);
			Descriptor ru = screenToDescriptor(new Vector2(width / 2 + drawingWidth / 2, height / 2 + drawingHeight / 2), tmpzoom);
			for (int i = lo.X; i <= ru.X; i++)
			{
				for (int j = lo.Y; j <= ru.Y; j++)
				{
					Descriptor desc = new Descriptor(i, j, tmpzoom);
					TileGL tile = null;
					try
					{
						tile = loadedTiles.get(desc.GetHashCode());
					}
					catch (Exception ex)
					{
					}
					if (tile != null)
					{
						// das Alter der benutzten Tiles auf 0 setzen wenn dies
						// f�r den richtigen aktuellen Zoom ist
						if (tmpzoom == aktZoom) tile.Age = 0;

						if (!tilesToDraw.containsKey(tile.Descriptor.GetHashCode())) tilesToDraw.put(tile.Descriptor.GetHashCode(), tile);
					}
					else if (tmpzoom == aktZoom)
					{
						// f�r den aktuellen Zoom ist kein Tile vorhanden ->
						// kleinere Zoomfaktoren durchsuchen
						if (!renderBiggerTiles(batch, i, j, aktZoom))
						// gr��ere Zoomfaktoren noch durchsuchen, ob davon Tiles
						// vorhanden sind...
						// daf�r m�ssen aber pro fehlendem Tile mehrere kleine
						// Tiles gezeichnet werden (4 oder 16 oder 64...)
						// dieser Aufruf kann auch rekursiv sein...
						renderSmallerTiles(batch, i, j, aktZoom);
					}
				}
			}
		}
		for (TileGL tile : tilesToDraw.values())
		{
			tile.createTexture();
			// Faktor, mit der dieses MapTile vergr��ert gezeichnet
			// werden mu�
			long posFactor = getMapTilePosFactor(tile.Descriptor.Zoom);

			float xPos = tile.Descriptor.X * posFactor * 256;
			float yPos = -(tile.Descriptor.Y + 1) * posFactor * 256;
			float xSize = tile.texture.getWidth() * posFactor;
			float ySize = tile.texture.getHeight() * posFactor;
			batch.draw(tile.texture, xPos, yPos, xSize, ySize);
		}
		tilesToDraw.clear();

		batch.end();
	}

	private void renderDebugInfo()
	{
		str = GlobalCore.FormatLatitudeDM(center.Latitude) + " - " + GlobalCore.FormatLongitudeDM(center.Longitude);
		Sizes.GL.fontAB18.draw(batch, str, 20, 120);

		str = "fps: " + Gdx.graphics.getFramesPerSecond();
		Sizes.GL.fontAB18.draw(batch, str, 20, 100);

		str = String.valueOf(aktZoom) + " - camzoom: " + Math.round(camera.zoom * 100) / 100;
		Sizes.GL.fontAB18.draw(batch, str, 20, 80);

		str = "lTiles: " + loadedTiles.size() + " - qTiles: " + queuedTiles.size();
		Sizes.GL.fontAB18.draw(batch, str, 20, 60);

		if (mapCacheList != null)
		{
			str = "listCalc: " + mapCacheList.anz + " - C: " + mapCacheList.list.size();
			Sizes.GL.fontAB18.draw(batch, str, 20, 40);
		}
		str = "lastMove: " + lastMovement.x + " - " + lastMovement.y;
		Sizes.GL.fontAB18.draw(batch, str, 20, 20);

	}

	private void renderInfoPanel()
	{
		// draw background
		Sprite sprite = SpriteCache.InfoBack;
		sprite.setPosition(Sizes.GL.Info.getX(), Sizes.GL.Info.getY());
		sprite.setSize(Sizes.GL.Info.getWidth(), Sizes.GL.Info.getHeight());
		sprite.draw(MapViewGlListener.batch);

		// Position ist entweder GPS-Position oder die des Markers, wenn
		// dieser gesetzt wurde.
		Coordinate position = null;
		if ((GlobalCore.Marker != null) && (GlobalCore.Marker.Valid)) position = GlobalCore.Marker;
		else if (GlobalCore.LastValidPosition != null) position = GlobalCore.LastValidPosition;
		else
			position = new Coordinate();

		// Gps empfang ?
		if (GlobalCore.SelectedCache() != null && position.Valid)
		{
			// Distanz einzeichnen
			float distance = 0;

			if (GlobalCore.SelectedWaypoint() == null) distance = position.Distance(GlobalCore.SelectedCache().Pos);
			else
				distance = position.Distance(GlobalCore.SelectedWaypoint().Pos);

			String text = UnitFormatter.DistanceString(distance);
			Sizes.GL.fontAB18.draw(batch, text, Sizes.GL.InfoLine1.x, Sizes.GL.InfoLine1.y);
			// canvas.drawText(text, leftString, bottom - 10, paint);

			// Kompassnadel zeichnen
			if (Global.Locator != null)
			{
				Coordinate cache = (GlobalCore.SelectedWaypoint() != null) ? GlobalCore.SelectedWaypoint().Pos
						: GlobalCore.SelectedCache().Pos;
				double bearing = Coordinate.Bearing(position.Latitude, position.Longitude, cache.Latitude, cache.Longitude);
				double relativeBearing = bearing - Global.Locator.getHeading();
				// double relativeBearingRad = relativeBearing * Math.PI / 180.0;

				// draw compass
				Sprite compass = SpriteCache.MapArrows.get(0);
				compass.setRotation((float) -relativeBearing);
				compass.setBounds(Sizes.GL.Compass.getX(), Sizes.GL.Compass.getY(), Sizes.GL.Compass.getWidth(),
						Sizes.GL.Compass.getHeight());
				compass.setOrigin(Sizes.GL.halfCompass, Sizes.GL.halfCompass);
				compass.draw(MapViewGlListener.batch);

			}

			// Koordinaten
			if (position.Valid)
			{
				String textLatitude = GlobalCore.FormatLatitudeDM(position.Latitude);
				String textLongitude = GlobalCore.FormatLongitudeDM(position.Longitude);

				Sizes.GL.fontAB18.draw(batch, textLatitude, Sizes.GL.InfoLine2.x, Sizes.GL.InfoLine1.y);
				Sizes.GL.fontAB18.draw(batch, textLongitude, Sizes.GL.InfoLine2.x, Sizes.GL.InfoLine2.y);

				if (Global.Locator != null)
				{
					Sizes.GL.fontAB18.draw(batch, Global.Locator.SpeedString(), Sizes.GL.InfoLine1.x, Sizes.GL.InfoLine2.y);
				}

			}
		}

	}

	private void renderPositionMarker()
	{
		if (Global.Locator != null)
		{
			PointD point = Descriptor.ToWorld(Descriptor.LongitudeToTileX(maxMapZoom, GlobalCore.LastValidPosition.Longitude),
					Descriptor.LatitudeToTileY(maxMapZoom, GlobalCore.LastValidPosition.Latitude), maxMapZoom, maxMapZoom);

			Vector2 vPoint = new Vector2((float) point.X, -(float) point.Y);

			Vector2 screen = worldToScreen(vPoint);

			boolean lastUsedCompass = Global.Locator.LastUsedCompass;
			boolean Transparency = Config.settings.PositionMarkerTransparent.getValue();
			// int arrowId = lastUsedCompass ? (Transparency ? 2 : 0) :
			// (Transparency ? 3 : 1);
			int arrowId = 0;
			if (lastUsedCompass)
			{
				arrowId = Transparency ? 1 : 0;
			}
			else
			{
				arrowId = Transparency ? 3 : 2;
			}

			Sprite arrow = SpriteCache.MapArrows.get(arrowId);
			arrow.setRotation(-arrowHeading);
			arrow.setBounds(screen.x - Sizes.GL.halfPosMarkerSize, screen.y - Sizes.GL.halfPosMarkerSize, Sizes.GL.PosMarkerSize,
					Sizes.GL.PosMarkerSize);
			arrow.setOrigin(Sizes.GL.halfPosMarkerSize, Sizes.GL.halfPosMarkerSize);
			arrow.draw(batch);
		}
	}

	private void renderWPs(SizeF WpUnderlay, SizeF WpSize)
	{
		if (mapCacheList.list != null)
		{
			synchronized (mapCacheList.list)
			{
				for (WaypointRenderInfo wpi : mapCacheList.list)
				{
					Vector2 screen = worldToScreen(new Vector2(wpi.MapX, wpi.MapY));

					float NameYMovement = 0;

					if ((aktZoom >= zoomCross) && (wpi.Selected) && (wpi.Waypoint == GlobalCore.SelectedWaypoint()))
					{
						// Draw Cross and move screen vector
						Sprite cross = SpriteCache.MapOverlay.get(3);
						cross.setBounds(screen.x - WpUnderlay.halfWidth, screen.y - WpUnderlay.halfHeight, WpUnderlay.width,
								WpUnderlay.height);
						cross.draw(batch);

						screen.add(-WpUnderlay.width, WpUnderlay.height);
						NameYMovement = WpUnderlay.height;
					}

					if (wpi.UnderlayIcon != null)
					{
						wpi.UnderlayIcon.setBounds(screen.x - WpUnderlay.halfWidth, screen.y - WpUnderlay.halfHeight, WpUnderlay.width,
								WpUnderlay.height);
						wpi.UnderlayIcon.draw(batch);
					}
					if (wpi.Icon != null)
					{
						wpi.Icon.setBounds(screen.x - WpSize.halfWidth, screen.y - WpSize.halfHeight, WpSize.width, WpSize.height);
						wpi.Icon.draw(batch);
					}

					if (wpi.OverlayIcon != null)
					{
						wpi.OverlayIcon.setBounds(screen.x - WpUnderlay.halfWidth, screen.y - WpUnderlay.halfHeight, WpUnderlay.width,
								WpUnderlay.height);
						wpi.OverlayIcon.draw(batch);
					}

					boolean drawAsWaypoint = wpi.Waypoint != null;

					// Rating des Caches darstellen
					if (showRating && (!drawAsWaypoint) && (wpi.Cache.Rating > 0) && (aktZoom >= 15))
					{
						Sprite rating = SpriteCache.MapStars.get((int) Math.min(wpi.Cache.Rating * 2, 5 * 2));
						rating.setBounds(screen.x - WpUnderlay.halfWidth, screen.y - WpUnderlay.halfHeight - WpUnderlay.Height4_8,
								WpUnderlay.width, WpUnderlay.Height4_8);
						rating.setOrigin(WpUnderlay.width / 2, WpUnderlay.Height4_8 / 2);
						rating.setRotation(0);
						rating.draw(batch);
						NameYMovement += WpUnderlay.Height4_8;
					}

					// Beschriftung
					if (showTitles && (aktZoom >= 15) && (!drawAsWaypoint))
					{
						float halfWidth = Sizes.GL.fontAB16out.getBounds(wpi.Cache.Name).width / 2;
						Sizes.GL.fontAB16out.draw(batch, wpi.Cache.Name, screen.x - halfWidth, screen.y - WpUnderlay.halfHeight
								- NameYMovement);
					}

					// Show D/T-Rating
					if (showDT && (!drawAsWaypoint) && (aktZoom >= 15))
					{
						Sprite difficulty = SpriteCache.MapStars.get((int) Math.min(wpi.Cache.Difficulty * 2, 5 * 2));
						difficulty.setBounds(screen.x - WpUnderlay.width - Sizes.GL.infoShadowHeight,
								screen.y - (WpUnderlay.Height4_8 / 2), WpUnderlay.width, WpUnderlay.Height4_8);
						difficulty.setOrigin(WpUnderlay.width / 2, WpUnderlay.Height4_8 / 2);
						difficulty.setRotation(90);
						difficulty.draw(batch);

						Sprite terrain = SpriteCache.MapStars.get((int) Math.min(wpi.Cache.Terrain * 2, 5 * 2));
						terrain.setBounds(screen.x + Sizes.GL.infoShadowHeight, screen.y - (WpUnderlay.Height4_8 / 2), WpUnderlay.width,
								WpUnderlay.Height4_8);
						terrain.setOrigin(WpUnderlay.width / 2, WpUnderlay.Height4_8 / 2);
						terrain.setRotation(90);
						terrain.draw(batch);

					}

					if ((wpi.Cache.Id == Bubble.CacheId) && (wpi.Waypoint == Bubble.waypoint) && Bubble.isShow)
					{
						Bubble.Pos = screen;
					}

				}
			}
		}

	}

	private boolean renderBiggerTiles(SpriteBatch batch2, int i, int j, int zoom2)
	{
		// f�r den aktuellen Zoom ist kein Tile vorhanden -> kleinere
		// Zoomfaktoren noch durchsuchen, ob davon Tiles vorhanden sind...
		// von dem gefundenen Tile mu� dann nur ein Ausschnitt gezeichnet werden
		int ii = i / 2;
		int jj = j / 2;
		int zoomzoom = zoom2 - 1;

		Descriptor desc = new Descriptor(ii, jj, zoomzoom);
		TileGL tile = null;
		try
		{
			tile = loadedTiles.get(desc.GetHashCode());
		}
		catch (Exception ex)
		{
		}
		if (tile != null)
		{
			// das Alter der benutzten Tiles nicht auf 0 setzen, da dies
			// eigentlich nicht das richtige Tile ist!!!
			// tile.Age = 0;
			if (!tilesToDraw.containsKey(tile.Descriptor.GetHashCode())) tilesToDraw.put(tile.Descriptor.GetHashCode(), tile);
			return true;
		}
		else if ((zoomzoom >= aktZoom - 3) && (zoomzoom >= zoomBtn.getMinZoom()))
		{
			// f�r den aktuellen Zoom ist kein Tile vorhanden -> gr��ere
			// Zoomfaktoren noch durchsuchen, ob davon Tiles vorhanden
			// sind...
			// daf�r m�ssen aber pro fehlendem Tile mehrere kleine Tiles
			// gezeichnet werden (4 oder 16 oder 64...)
			// dieser Aufruf kann auch rekursiv sein...
			renderBiggerTiles(batch, ii, jj, zoomzoom);
		}
		return false;
	}

	private void renderSmallerTiles(SpriteBatch batch2, int i, int j, int zoom2)
	{
		// f�r den aktuellen Zoom ist kein Tile vorhanden -> gr��ere
		// Zoomfaktoren noch durchsuchen, ob davon Tiles vorhanden sind...
		// daf�r m�ssen aber pro fehlendem Tile mehrere kleine Tiles gezeichnet
		// werden (4 oder 16 oder 64...)
		int i1 = i * 2;
		int i2 = i * 2 + 1;
		int j1 = j * 2;
		int j2 = j * 2 + 1;
		int zoomzoom = zoom2 + 1;
		for (int ii = i1; ii <= i2; ii++)
		{
			for (int jj = j1; jj <= j2; jj++)
			{
				Descriptor desc = new Descriptor(ii, jj, zoomzoom);
				TileGL tile = null;
				try
				{
					tile = loadedTiles.get(desc.GetHashCode());
				}
				catch (Exception ex)
				{
				}
				if (tile != null)
				{
					if (!tilesToDraw.containsKey(tile.Descriptor.GetHashCode())) tilesToDraw.put(tile.Descriptor.GetHashCode(), tile);
					// das Alter der benutzten Tiles nicht auf 0 setzen, da dies
					// eigentlich nicht das richtige Tile ist!!!
					// tile.Age = 0;
				}
				else if ((zoomzoom <= aktZoom + 0) && (zoomzoom <= maxMapZoom))
				{
					// f�r den aktuellen Zoom ist kein Tile vorhanden -> gr��ere
					// Zoomfaktoren noch durchsuchen, ob davon Tiles vorhanden
					// sind...
					// daf�r m�ssen aber pro fehlendem Tile mehrere kleine Tiles
					// gezeichnet werden (4 oder 16 oder 64...)
					// dieser Aufruf kann auch rekursiv sein...
					renderSmallerTiles(batch, ii, jj, zoomzoom);
				}
			}
		}
	}

	private void loadTiles()
	{
		deleteUnusedTiles();
		// alle notwendigen Tiles zum Laden einstellen in die Queue
		// (queuedTiles)
		int extensionTop = width / 2;
		int extensionBottom = width / 2;
		int extensionLeft = width / 2;
		int extensionRight = width / 2;
		Descriptor lo = screenToDescriptor(new Vector2(width / 2 - drawingWidth / 2 - extensionLeft, height / 2 - drawingHeight / 2
				- extensionTop), aktZoom);
		Descriptor ru = screenToDescriptor(new Vector2(width / 2 + drawingWidth / 2 + extensionRight, height / 2 + drawingHeight / 2
				+ extensionBottom), aktZoom);

		mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, false);

		loadedTilesLock.lock();
		queuedTilesLock.lock();
		// Queue jedesmal l�schen, damit die Tiles, die eigentlich
		// mal
		// gebraucht wurden aber trotzdem noch nicht geladen sind
		// auch nicht mehr geladen werden
		queuedTiles.clear();
		try
		{
			for (int i = lo.X; i <= ru.X; i++)
			{
				for (int j = lo.Y; j <= ru.Y; j++)
				{
					Descriptor desc = new Descriptor(i, j, aktZoom);

					if (loadedTiles.containsKey(desc.GetHashCode()))
					{
						continue; // Dieses
									// Tile
									// existiert
									// schon!
					}
					if (queuedTiles.containsKey(desc.GetHashCode())) continue;
					queueTile(desc);
				}
			}
		}
		finally
		{
			queuedTilesLock.unlock();
			loadedTilesLock.unlock();
		}
	}

	private void deleteUnusedTiles()
	{
		// Ist Auslagerung �berhaupt n�tig?
		if (numLoadedTiles() <= maxNumTiles) return;
		// Wenn die Anzahl der maximal gleichzeitig geladenen Tiles
		// �berschritten ist
		// die �ltesten Tiles l�schen
		do
		{
			loadedTilesLock.lock();
			try
			{
				// Kachel mit maximalem Alter suchen
				long maxAge = Integer.MIN_VALUE;
				Descriptor maxDesc = null;

				for (TileGL tile : loadedTiles.values())
					if (/* tile.texture != null && */tile.Age > maxAge)
					{
						maxAge = tile.Age;
						maxDesc = tile.Descriptor;
					}

				// Instanz freigeben und Eintrag l�schen
				if (maxDesc != null)
				{
					try
					{
						TileGL tile = loadedTiles.get(maxDesc.GetHashCode());
						loadedTiles.remove(maxDesc.GetHashCode());
						tile.destroy();
					}
					catch (Exception ex)
					{
						Logger.Error("MapView.preemptTile()", "", ex);
					}
				}
			}
			finally
			{
				loadedTilesLock.unlock();
			}
		}
		while (numLoadedTiles() > maxNumTiles);
	}

	int numLoadedTiles()
	{
		return loadedTiles.size();
		// int cnt = 0;
		// loadedTilesLock.lock();
		// try
		// {
		// for (TileGL tile : loadedTiles.values())
		// if (tile.texture != null) cnt++;
		// }
		// finally
		// {
		// loadedTilesLock.unlock();
		// }
		//
		// return cnt;
	}

	@Override
	public void pause()
	{
		onStop();
	}

	@Override
	public void resume()
	{
		onStart();
	}

	@Override
	public void dispose()
	{
		SpriteCache.destroyCache();

	}

	public void onStart()
	{
		started.set(true);
	}

	public void onStop()
	{

		// TODO wenn der ScreenLock angezeigt wird, kommt es auch zu einem
		// onStop.
		// Es darf dann aber nicht gestoppt werden.
		// Die main als AndroidAplication stoppt hier in onPause() das rendern.
		// Abhilfe schafft hier nur das �ndern des gdx Codes!
		if (ScreenLock.isShown) return;

		started.set(false);
		for (TileGL tile : loadedTiles.values())
		{
			try
			{
				tile.destroy();
			}
			catch (DestroyFailedException e)
			{
				e.printStackTrace();
			}
		}
		loadedTiles.clear();
	}

	private void stateChanged()
	{
		if (btnTrackPos.getState() > 0)
		{
			setCenter(new Coordinate(GlobalCore.LastValidPosition.Latitude, GlobalCore.LastValidPosition.Longitude));
		}
	}

	class CameraController implements GestureListener
	{
		float velX, velY;
		boolean flinging = false;
		float initialScale = 1;

		public boolean touchDown(int x, int y, int pointer)
		{
			// Log.d("CACHEBOX", "touchDown pointer" + pointer);

			flinging = false;
			initialScale = camera.zoom;

			Vector2 touchdAt = new Vector2(Gdx.input.getX(), height - Gdx.input.getY());

			if (btnTrackPos.touchDownTest(touchdAt)) return false;
			if (zoomBtn.touchDownTest(touchdAt)) return false;

			return false;
		}

		@Override
		public boolean tap(int x, int y, int count)
		{
			TouchUp();
			// Log.d("CACHEBOX", "Tab count" + count);

			double minDist = Double.MAX_VALUE;
			WaypointRenderInfo minWpi = null;
			Vector2 clickedAt = new Vector2(Gdx.input.getX(), height - Gdx.input.getY());

			// check ToggleBtn clicked
			if (btnTrackPos.hitTest(clickedAt))
			{
				main.vibrate();
				stateChanged();
				forceRender();
				return true;
			}

			// check Zoom Button clicked
			if (zoomBtn.hitTest(clickedAt))
			{
				main.vibrate();
				// start Zoom f�r die Animation des camera.zoom
				startCameraZoom = camera.zoom;
				// dieser Zoom Faktor soll angestrebt werden
				endCameraZoom = getMapTilePosFactor(zoomBtn.getZoom());
				// Zoom Geschwindigkeit
				diffCameraZoom = Math.abs(endCameraZoom - startCameraZoom) / 20;
				// camera.zoom = getMapTilePosFactor(aktZoom);
				zoomScale.setZoom(zoomBtn.getZoom());
				zoomScale.resetFadeOut();

				forceRender();
				return true;
			}

			synchronized (mapCacheList.list)
			{
				// Bubble gedr�ckt?
				if ((Bubble.cache != null) && Bubble.isShow && Bubble.DrawRec != null && Bubble.DrawRec.contains(clickedAt.x, clickedAt.y))
				{
					// Click inside Bubble -> hide Bubble and select Cache
					// GlobalCore.SelectedWaypoint(Bubble.cache,
					// Bubble.waypoint);
					ThreadSaveSetSelectedWP(Bubble.cache, Bubble.waypoint);
					CacheDraw.ReleaseCacheBMP();
					Bubble.isShow = false;
					Bubble.CacheId = -1;
					Bubble.cache = null;
					Bubble.waypoint = null;
					mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, true);

					// Shutdown Autoresort
					Global.autoResort = false;
					forceRender();
					// do nothing else with this click
					return false;
				}
				else if (Bubble.isShow)
				{
					// Click outside Bubble -> hide Bubble
					Bubble.isShow = false;
					forceRender();
				}

				for (WaypointRenderInfo wpi : mapCacheList.list)
				{
					Vector2 screen = worldToScreen(new Vector2(Math.round(wpi.MapX), Math.round(wpi.MapY)));
					if (clickedAt != null)
					{
						double aktDist = Math.sqrt(Math.pow(screen.x - clickedAt.x, 2) + Math.pow(screen.y - clickedAt.y, 2));
						if (aktDist < minDist)
						{
							minDist = aktDist;
							minWpi = wpi;
						}
					}
				}
				// if (minDist < 40)
				// {
				//
				// final Cache updateCache = minWpi.Cache;
				// final Waypoint updateWaypoint = minWpi.Waypoint;
				//
				// ThreadSaveSetSelectedWP(updateCache, updateWaypoint);
				//
				// // CacheListe auf jeden Fall neu berechnen
				// // k�nnte aber noch verbessert werden, indem nur der letzte
				// // selected und der neue selected ge�ndert werden!
				// mapCacheList.update(screenToWorld(new Vector2(0, 0)),
				// screenToWorld(new Vector2(width, height)), zoom, true);
				//
				// }

				if (minWpi == null || minWpi.Cache == null) return false;

				if (minDist < 40)
				{

					if (minWpi.Waypoint != null)
					{
						if (GlobalCore.SelectedCache() != minWpi.Cache)
						{
							// Show Bubble
							Bubble.isShow = true;
							Bubble.CacheId = minWpi.Cache.Id;
							Bubble.cache = minWpi.Cache;
							Bubble.waypoint = minWpi.Waypoint;
							Bubble.disposeSprite();
							mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, true);

						}
						else
						{
							// do not show Bubble because there will not be
							// selected
							// a
							// different cache but only a different waypoint
							// Wegpunktliste ausrichten
							ThreadSaveSetSelectedWP(minWpi.Cache, minWpi.Waypoint);
							// FormMain.WaypointListPanel.AlignSelected();
							// updateCacheList();
							mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, true);
						}

					}
					else
					{
						if (GlobalCore.SelectedCache() != minWpi.Cache)
						{
							Bubble.isShow = true;
							Bubble.CacheId = minWpi.Cache.Id;
							Bubble.cache = minWpi.Cache;
							Bubble.disposeSprite();
							mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, true);
						}
						else
						{
							Bubble.isShow = true;
							Bubble.CacheId = minWpi.Cache.Id;
							Bubble.cache = minWpi.Cache;
							Bubble.disposeSprite();
							// Cacheliste ausrichten
							ThreadSaveSetSelectedWP(minWpi.Cache);
							// updateCacheList();
							mapCacheList.update(screenToWorld(new Vector2(0, 0)), screenToWorld(new Vector2(width, height)), aktZoom, true);
						}
					}
					forceRender();
				}
			}
			return false;
		}

		/**
		 * W�hlt Cache Thread sicher an.
		 * 
		 * @param cache
		 */
		private void ThreadSaveSetSelectedWP(final Cache cache)
		{
			ThreadSaveSetSelectedWP(cache, null);
		}

		/**
		 * W�hlt Cache und Waypoint Thread sicher an.
		 * 
		 * @param cache
		 * @param waypoint
		 */
		private void ThreadSaveSetSelectedWP(final Cache cache, final Waypoint waypoint)
		{
			Thread t = new Thread()
			{
				public void run()
				{
					main.mainActivity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if (waypoint == null)
							{
								GlobalCore.SelectedCache(cache);
							}
							else
							{
								GlobalCore.SelectedWaypoint(cache, waypoint);
							}
						}
					});
				}
			};

			t.start();
		}

		@Override
		public boolean longPress(int x, int y)
		{
			TouchUp();
			Gdx.app.log("GestureDetectorTest", "long press at " + x + ", " + y);
			return false;
		}

		@Override
		public boolean fling(float velocityX, float velocityY)
		{
			TouchUp();
			// Log.d("CACHEBOX", "velocity " + velocityX);

			if (btnTrackPos.getState() > 1) return false;

			flinging = true;

			Vector2 richtung = new Vector2(velocityX, velocityY);
			richtung.rotate(mapHeading);

			velX = camera.zoom * richtung.x * 0.5f;
			velY = camera.zoom * richtung.y * 0.5f;
			return false;
		}

		@Override
		public boolean pan(int x, int y, int deltaX, int deltaY)
		{
			TouchUp();
			// Ohne verschiebung brauch auch keine neue Pos berechnet werden!
			if (deltaX == 0 && deltaY == 0) return false;
			// Log.d("CACHEBOX", "pan " + deltaX);

			if (btnTrackPos.getState() > 1) return false;

			// Drehung der Karte ber�cksichtigen
			Vector2 richtung = new Vector2(deltaX, deltaY);
			richtung.rotate(mapHeading);
			camera.position.add(-richtung.x * camera.zoom, richtung.y * camera.zoom, 0);
			screenCenterW.x = camera.position.x;
			screenCenterW.y = camera.position.y;
			calcCenter();
			btnTrackPos.setState(0);
			return false;
		}

		private void calcCenter()
		{
			// berechnet anhand des ScreenCenterW die Center-Coordinaten
			PointD point = Descriptor.FromWorld(screenCenterW.x, screenCenterW.y, maxMapZoom, maxMapZoom);

			center = new Coordinate(Descriptor.TileYToLatitude(maxMapZoom, -point.Y), Descriptor.TileXToLongitude(maxMapZoom, point.X));
		}

		@Override
		public boolean zoom(float originalDistance, float currentDistance)
		{

			boolean positive = true;
			Log.d("CACHEBOX", "pan " + originalDistance + "  |  " + currentDistance);
			float ratio = originalDistance / currentDistance;
			camera.zoom = initialScale * ratio;
			endCameraZoom = camera.zoom;
			System.out.println(camera.zoom);
			int zoom = maxMapZoom;
			float tmpZoom = camera.zoom;
			float faktor = 1.5f;
			faktor = faktor - iconFactor + 1;
			while (tmpZoom > faktor)
			{
				tmpZoom /= 2;
				zoom--;
			}
			zoomBtn.setZoom(zoom);
			zoomScale.resetFadeOut();
			zoomScale.setZoom(zoom);
			zoomScale.setDiffCameraZoom(1 - (tmpZoom * 2), positive);

			return false;
		}

		public void update()
		{
			if (flinging)
			{
				velX *= 0.98f;
				velY *= 0.98f;
				camera.position.add(-velX * Gdx.graphics.getDeltaTime(), velY * Gdx.graphics.getDeltaTime(), 0);
				if (Math.abs(velX) < 0.01f) velX = 0;
				if (Math.abs(velY) < 0.01f) velY = 0;
				screenCenterW.x = camera.position.x;
				screenCenterW.y = camera.position.y;
				calcCenter();
			}
		}

		public void TouchUp()
		{
			btnTrackPos.TouchRelease();
			zoomBtn.TouchRelease();
		}

	}

	public void Initialize()
	{
		// minzoom = Config.settings.OsmMinLevel.getValue();
		// maxzoom = Config.settings.OsmMaxLevel.getValue();

	}

	public void InitializeMap()
	{
		zoomCross = Config.settings.ZoomCross.getValue();
		zoomBtn.setZoom(Config.settings.lastZoomLevel.getValue());
		// Bestimmung der ersten Position auf der Karte
		if (!positionInitialized)
		{
			double lat = Config.settings.MapInitLatitude.getValue();
			double lon = Config.settings.MapInitLongitude.getValue();

			// Initialisierungskoordinaten bekannt und k�nnen �bernommen werden
			if (lat != -1000 && lon != -1000)
			{
				setCenter(new Coordinate(lat, lon));
				positionInitialized = true;
				// setLockPosition(0);
			}
			else
			{
				// GPS-Position bekannt?
				if (GlobalCore.LastValidPosition.Valid)
				{
					setCenter(new Coordinate(GlobalCore.LastValidPosition));
					positionInitialized = true;
				}
				else
				{
					try
					{
						if (Database.Data.Query != null)
						{
							if (Database.Data.Query.size() > 0)
							{
								// Koordinaten des ersten Caches der Datenbank
								// nehmen
								setCenter(new Coordinate(Database.Data.Query.get(0).Latitude(), Database.Data.Query.get(0).Longitude()));
								positionInitialized = true;
								// setLockPosition(0);
							}
							else
							{
								// Wenns auch den nicht gibt...)
								setCenter(new Coordinate(48.0, 12.0));
							}
						}
						else
						{
							// Wenn Query == null
							setCenter(new Coordinate(48.0, 12.0));
						}
					}
					catch (Exception e)
					{
						setCenter(new Coordinate(48.0, 12.0));
						e.printStackTrace();
					}

				}
			}

			// Gr��e des Ma�stabes berechnen etc...
			// zoomChanged();
		}

		setNewSettings();

	}

	public void setNewSettings()
	{
		showRating = Config.settings.MapShowRating.getValue();
		showDT = Config.settings.MapShowDT.getValue();
		showTitles = Config.settings.MapShowTitles.getValue();
		hideMyFinds = Config.settings.MapHideMyFinds.getValue();
		showCompass = Config.settings.MapShowCompass.getValue();
		showDirektLine = Config.settings.ShowDirektLine.getValue();
		nightMode = Config.settings.nightMode.getValue();
		iconFactor = (float) Config.settings.MapViewDPIFaktor.getValue();
		aktZoom = Config.settings.lastZoomLevel.getValue();
		zoomBtn.setMaxZoom(Config.settings.OsmMaxLevel.getValue());
		zoomBtn.setMinZoom(Config.settings.OsmMinLevel.getValue());
		zoomBtn.setZoom(aktZoom);

		zoomScale.setMaxZoom(Config.settings.OsmMaxLevel.getValue());
		zoomScale.setMinZoom(Config.settings.OsmMinLevel.getValue());
		zoomScale.setZoom(aktZoom);
	}

	private void setScreenCenter(Vector2 newCenter)
	{
		screenCenterW.x = newCenter.x;
		screenCenterW.y = -newCenter.y;
		if (camera != null) camera.position.set((float) screenCenterW.x, (float) screenCenterW.y, 0);
	}

	public void setCenter(Coordinate value)
	{
		synchronized (screenCenterW)
		{

			if (center == null) center = new Coordinate(48.0, 12.0);
			positionInitialized = true;
			/*
			 * if (animationTimer != null) animationTimer.Enabled = false;
			 */
			if (center == value) return;

			center = value;

			PointD point = Descriptor.ToWorld(Descriptor.LongitudeToTileX(maxMapZoom, center.Longitude),
					Descriptor.LatitudeToTileY(maxMapZoom, center.Latitude), maxMapZoom, maxMapZoom);

			setScreenCenter(new Vector2((float) point.X, (float) point.Y));
		}
	}

	private long getMapTileSizeFactor(int zoom)
	{
		long result = 1;
		for (int z = maxMapZoom; z < zoom; z++)
		{
			result *= 2;
		}
		return result;
	}

	private long getMapTilePosFactor(int zoom)
	{
		long result = 1;
		for (int z = zoom; z < maxMapZoom; z++)
		{
			result *= 2;
		}
		return result;
	}

	/**
	 * liefert die World-Koordinate in Pixel relativ zur Map in der h�chsten Aufl�sung
	 */
	private Vector2 screenToWorld(Vector2 point)
	{
		Vector2 result = new Vector2(0, 0);
		result.x = screenCenterW.x + (point.x - width / 2) * camera.zoom;
		result.y = -screenCenterW.y + (point.y - height / 2) * camera.zoom;
		return result;
	}

	private Vector2 worldToScreen(Vector2 point)
	{
		Vector2 result = new Vector2(0, 0);
		result.x = (point.x - screenCenterW.x) / camera.zoom + width / 2;
		result.y = -(-point.y + screenCenterW.y) / camera.zoom + height / 2;
		result.add(-width / 2, -height / 2);
		result.rotate(mapHeading);
		result.add(width / 2, height / 2);
		return result;
	}

	private Descriptor screenToDescriptor(Vector2 point, int zoom)
	{
		// World-Koordinaten in Pixel
		Vector2 world = screenToWorld(point);
		for (int i = maxMapZoom; i > zoom; i--)
		{
			world.x /= 2;
			world.y /= 2;
		}
		world.x /= 256;
		world.y /= 256;
		int x = (int) world.x;
		int y = (int) world.y;
		Descriptor result = new Descriptor(x, y, zoom);
		return result;
	}

	@Override
	public void PositionChanged(Location location)
	{
		GlobalCore.LastValidPosition = new Coordinate(location.getLatitude(), location.getLongitude());
		GlobalCore.LastValidPosition.Elevation = location.getAltitude();

		if (btnTrackPos.getState() > 0) setCenter(new Coordinate(location.getLatitude(), location.getLongitude()));
		forceRender();
	}

	@Override
	public void OrientationChanged(float heading)
	{
		// liefert die Richtung (abh�ngig von der Geschwindigkeit von
		// Kompass oder GPS
		if (!Global.Locator.UseCompass())
		{
			// GPS-Richtung soll verwendet werden!
			heading = Global.Locator.getHeading();
		}

		if (alignToCompass)
		{
			this.mapHeading = heading;
			this.arrowHeading = 0;

			// da die Map gedreht in die offScreenBmp gezeichnet werden soll,
			// muss der Bereich, der gezeichnet werden soll gr��er sein, wenn
			// gedreht wird.
			if (heading >= 180) heading -= 180;
			if (heading > 90) heading = 180 - heading;
			double alpha = heading / 180 * Math.PI;
			double beta = Math.atan((double) width / (double) height);
			double gammaW = Math.PI / 2 - alpha - beta;
			// halbe L�nge der Diagonalen
			double diagonal = Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)) / 2;
			drawingWidth = (int) (Math.cos(gammaW) * diagonal * 2);

			double gammaH = alpha - beta;
			drawingHeight = (int) (Math.cos(gammaH) * diagonal * 2);
			forceRender();
		}
		else
		{
			this.mapHeading = 0;
			this.arrowHeading = heading;
			drawingWidth = width;
			drawingHeight = height;
		}

	}

	public void SetAlignToCompass(boolean value)
	{
		alignToCompass = value;
		if (!value)
		{
			drawingWidth = width;
			drawingHeight = height;
		}
	}

	public boolean GetCenterGps()
	{
		return (btnTrackPos.getState() > 0);
	}

	public boolean GetAlignToCompass()
	{
		return alignToCompass;
	}

	@SuppressWarnings("unchecked")
	private void queueTile(Descriptor desc)
	{
		// Alternative Implementierung mit Threadpools...
		// ThreadPool.QueueUserWorkItem(new WaitCallback(LoadTile), new
		// Descriptor(desc));

		queuedTilesLock.lock();
		try
		{
			if (queuedTiles.containsKey(desc.GetHashCode())) return;

			queuedTiles.put(desc.GetHashCode(), desc);
		}
		finally
		{
			queuedTilesLock.unlock();
		}

	}

	private class queueProcessor extends AsyncTask<Integer, Integer, Integer>
	{

		@Override
		protected Integer doInBackground(Integer... params)
		{
			boolean queueEmpty = false;
			try
			{
				do
				{
					Descriptor desc = null;
					if (queuedTiles.size() > 0)
					{
						try
						{
							queuedTilesLock.lock();
							try
							{
								// ArrayList<KachelOrder> kOrder = new
								// ArrayList<KachelOrder>();
								// long posFactor = getMapTilePosFactor(zoom);
								// for (int i = lo.X; i <= ru.X; i++)
								// {
								// for (int j = lo.Y; j <= ru.Y; j++)
								// {
								// Descriptor desc = new Descriptor(i, j, zoom);
								// double dist = Math.sqrt(Math.pow((double)
								// desc.X * posFactor * 256 +
								// 128 - screenCenterW.X, 2)
								// + Math.pow((double) desc.Y * posFactor * 256
								// - 128 + screenCenterW.Y,
								// 2));
								// kOrder.add(new KachelOrder(desc, dist));
								// }
								// }
								// Collections.sort(kOrder);

								Descriptor nearestDesc = null;
								double nearestDist = Double.MAX_VALUE;
								int nearestZoom = 0;
								for (Descriptor tmpDesc : queuedTiles.values())
								{
									long posFactor = getMapTilePosFactor(tmpDesc.Zoom);

									double dist = Math.sqrt(Math.pow((double) tmpDesc.X * posFactor * 256 + 128 * posFactor
											- screenCenterW.x, 2)
											+ Math.pow((double) tmpDesc.Y * posFactor * 256 + 128 * posFactor + screenCenterW.y, 2));

									if (Math.abs(aktZoom - nearestZoom) > Math.abs(aktZoom - tmpDesc.Zoom))
									{
										// der Zoomfaktor des bisher besten
										// Tiles ist weiter entfernt vom
										// aktuellen Zoom als der vom tmpDesc ->
										// tmpDesc verwenden
										nearestDist = dist;
										nearestDesc = tmpDesc;
										nearestZoom = tmpDesc.Zoom;
									}

									if (dist < nearestDist)
									{
										if (Math.abs(aktZoom - nearestZoom) < Math.abs(aktZoom - tmpDesc.Zoom))
										{
											// zuerst die Tiles, die dem
											// aktuellen Zoom Faktor am n�chsten
											// sind.
											continue;
										}
										nearestDist = dist;
										nearestDesc = tmpDesc;
										nearestZoom = tmpDesc.Zoom;
									}
								}
								desc = nearestDesc;

							}
							finally
							{
								queuedTilesLock.unlock();
							}

							// if (desc.Zoom == zoom)
							{
								LoadTile(desc);
							}
						}
						catch (Exception ex1)
						{
							Logger.Error("MapViewGL.queueProcessor.doInBackground()", "1", ex1);
						}
					}
					else
					{
						Thread.sleep(100);
					}
				}
				while (true);
			}
			catch (Exception ex3)
			{
				Logger.Error("MapViewGL.queueProcessor.doInBackground()", "3", ex3);
			}
			finally
			{
				// damit im Falle einer Exception der Thread neu gestartet wird
				// queueProcessor = null;
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			queueProcessor = null;
		}
	}

	private void LoadTile(Descriptor desc)
	{
		TileGL.TileState tileState = TileGL.TileState.Disposed;

		byte[] bytes = MapView.Manager.LoadLocalPixmap(CurrentLayer, desc);
		// Texture texture = new Texture(new Pixmap(bytes, 0, bytes.length));
		tileState = TileGL.TileState.Present;
		addLoadedTile(desc, bytes, tileState);
	}

	private void addLoadedTile(Descriptor desc, byte[] bytes, TileGL.TileState state)
	{
		loadedTilesLock.lock();
		try
		{
			if (loadedTiles.containsKey(desc.GetHashCode()))
			{

			}
			else
			{
				TileGL tile = new TileGL(desc, bytes, state);
				loadedTiles.put(desc.GetHashCode(), tile);
			}

		}
		finally
		{
			loadedTilesLock.unlock();
		}

		queuedTilesLock.lock();
		try
		{
			if (queuedTiles.containsKey(desc.GetHashCode())) queuedTiles.remove(desc.GetHashCode());
		}
		finally
		{
			queuedTilesLock.unlock();
		}

	}

	private class KachelOrder implements Comparable<KachelOrder>
	{
		Descriptor desc;
		double dist;

		private KachelOrder(Descriptor desc, double dist)
		{
			this.desc = desc;
			this.dist = dist;
		}

		@Override
		public int compareTo(KachelOrder arg0)
		{
			return (dist < arg0.dist ? -1 : (dist == arg0.dist ? 0 : 1));
		}
	}

}
