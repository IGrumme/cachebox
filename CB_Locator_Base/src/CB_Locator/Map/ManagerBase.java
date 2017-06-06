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
package CB_Locator.Map;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.IDatabaseRenderer;
import org.mapsforge.map.layer.renderer.MF_DatabaseRenderer;
import org.mapsforge.map.layer.renderer.MixedDatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.CB_RenderThemeHandler;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.slf4j.LoggerFactory;

import CB_Locator.LocatorSettings;
import CB_Locator.Map.Layer.MapType;
import CB_Locator.Map.Layer.Type;
import CB_UI_Base.GL_UI.Controls.PopUps.ConnectionError;
import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.graphics.GL_RenderType;
import CB_Utils.Log.Log;
import CB_Utils.Util.FileIO;
import CB_Utils.Util.HSV_Color;
import CB_Utils.Util.IChanged;
import CB_Utils.fileProvider.File;
import CB_Utils.fileProvider.FileFactory;

/**
 * @author ging-buh
 * @author Longri
 */
public abstract class ManagerBase {

	final static org.slf4j.Logger log = LoggerFactory.getLogger(ManagerBase.class);

	public static ManagerBase Manager = null;

	public static int PROCESSOR_COUNT; // == nr of threads for getting tiles (mapsforge)
	private final DisplayModel DISPLAY_MODEL;

	private final int CONECTION_TIME_OUT = 15000;// 15 sec
	private final int CONECTION_TIME_OUT_MESSAGE_INTERVALL = 60000;// 1min

	public static long NumBytesLoaded = 0;
	public static int NumTilesLoaded = 0;
	public static int NumTilesCached = 0;

	public ArrayList<PackBase> mapPacks = new ArrayList<PackBase>();

	public ArrayList<TmsMap> tmsMaps = new ArrayList<TmsMap>();

	public ArrayList<Layer> layers = new ArrayList<Layer>();

	private final DefaultLayerList DEFAULT_LAYER = new DefaultLayerList();

	private boolean mayAddLayer = false; // add only during startup (why?)

	public ManagerBase(DisplayModel displaymodel) {
		Manager = this;
		//PROCESSOR_COUNT = 1; // = Runtime.getRuntime().availableProcessors();
		PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
		DISPLAY_MODEL = displaymodel;

		LocatorSettings.CurrentMapLayer.addChangedEventListener(new IChanged() {
			@Override
			public void isChanged() {
				Layer layer = getOrAddLayer(LocatorSettings.CurrentMapLayer.getValue(), "", "");
				if (layer.isMapsForge())
					initMapDatabase(layer);
			}
		});
	}

	public abstract PackBase CreatePack(String file) throws IOException;

	/**
	 * Läd ein Map Pack und fügt es dem Manager hinzu
	 * 
	 * @param file
	 * @return
	 * true, falls das Pack erfolgreich geladen wurde, sonst false
	 */
	public boolean LoadMapPack(String file) {
		try {
			PackBase pack = CreatePack(file);
			layers.add(pack.layer);
			mapPacks.add(pack);
			// Nach Aktualität sortieren
			Collections.sort(mapPacks);
			return true;
		} catch (Exception exc) {
		}
		return false;
	}

	public Layer getOrAddLayer(String[] Name, String friendlyName, String url) {
		if (Name[0] == "OSM" || Name[0] == "")
			Name[0] = "Mapnik";

		for (Layer layer : layers) {
			if (layer.Name.equalsIgnoreCase(Name[0])) {

				//add aditional
				if (Name.length > 1) {
					for (int i = 1; i < Name.length; i++) {
						for (Layer la : layers) {
							if (la.Name.equalsIgnoreCase(Name[i])) {
								layer.addMapsforgeLayer(la);
							}
						}
					}
				}
				return layer;
			}
		}

		if (mayAddLayer) {
			Layer newLayer = new Layer(MapType.ONLINE, Type.normal, Name[0], Name[0], url);
			layers.add(newLayer);
			return newLayer;
		} else {
			if (layers != null && layers.size() > 0) {
				LocatorSettings.CurrentMapLayer.setValue(layers.get(0).getNames());
				return layers.get(0); // ist wahrscheinlich Mapnik und sollte immer tun
			}
			return null;
		}
	}

	public class ImageData {
		public int[] PixelColorArray;
		public int width;
		public int height;
	}

	public abstract TileGL LoadLocalPixmap(Layer layer, Descriptor desc, int ThreadIndex);

	protected abstract ImageData getImagePixel(byte[] img);

	protected abstract byte[] getImageFromData(ImageData imgData);

	/**
	 * Load Tile from URL and save to MapTile-Cache
	 *
	 * @param layer
	 * @param tile
	 * @return
	 */
	public boolean cacheTile(Layer layer, Descriptor tile) {

		if (layer == null)
			return false;

		// Gibts die Kachel schon in einem Mappack? Dann kann sie übersprungen werden!
		for (PackBase pack : mapPacks)
			if (pack.layer == layer)
				if (pack.Contains(tile) != null)
					return true;

		String filename = layer.GetLocalFilename(tile);
		// String path = layer.GetLocalPath(tile);
		String url = layer.GetUrl(tile);

		// Falls Kachel schon geladen wurde, kann sie übersprungen werden
		synchronized (this) {
			if (FileIO.FileExists(filename))
				return true;
		}

		// Kachel laden
		// set the connection timeout value to 15 seconds (15000 milliseconds)
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CONECTION_TIME_OUT);
		HttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpGet GET = new HttpGet(url);
		GET.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		try {
			HttpResponse response = httpclient.execute(GET);
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();

				synchronized (this) {
					// Verzeichnis anlegen
					if (!FileIO.createDirectory(filename))
						return false;

					// Datei schreiben
					FileOutputStream stream = new FileOutputStream(filename, false);
					out.writeTo(stream);
					stream.close();
				}

				NumTilesLoaded++;
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				// throw new IOException(statusLine.getReasonPhrase());
				Log.err(log, url + ": " + response.getStatusLine());
				return false;
			}
			/*
			 * webRequest = (HttpWebRequest)WebRequest.Create(url); webRequest.Timeout = 15000; webRequest.Proxy = Global.Proxy; webResponse
			 * = webRequest.GetResponse(); if (!webRequest.HaveResponse) return false; responseStream = webResponse.GetResponseStream();
			 * byte[] result = Global.ReadFully(responseStream, 64000); // Verzeichnis anlegen lock (this) if (!Directory.Exists(path))
			 * Directory.CreateDirectory(path); // Datei schreiben lock (this) { stream = new FileStream(filename, FileMode.CreateNew);
			 * stream.Write(result, 0, result.Length); } NumTilesLoaded++; Global.TransferredBytes += result.Length;
			 */
		} catch (Exception ex) {
			// Check last Error for this URL and post massage if the last > 1 min.

			String URL = GET.getURI().getAuthority();

			boolean PostErrorMassage = false;

			if (LastRequestTimeOut.containsKey(URL)) {
				long last = LastRequestTimeOut.get(URL);
				if ((last + CONECTION_TIME_OUT_MESSAGE_INTERVALL) < System.currentTimeMillis()) {
					PostErrorMassage = true;
					LastRequestTimeOut.remove(URL);
				}
			} else {
				PostErrorMassage = true;
			}

			if (PostErrorMassage) {
				LastRequestTimeOut.put(URL, System.currentTimeMillis());
				ConnectionError INSTANCE = new ConnectionError(layer.Name + " - Provider");
				GL.that.Toast(INSTANCE);
			}

			return false;
		}
		/*
		 * finally { if (stream != null) { stream.Close(); stream = null; } if (responseStream != null) { responseStream.Close();
		 * responseStream = null; } if (webResponse != null) { webResponse.Close(); webResponse = null; } if (webRequest != null) {
		 * webRequest.Abort(); webRequest = null; } GC.Collect(); }
		 */

		return true;
	}

	HashMap<String, Long> LastRequestTimeOut = new HashMap<String, Long>();

	/**
	 * for night modus
	 * 
	 * The matrix is stored in a single array, and its treated as follows: [ a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t ] <br>
	 * <br>
	 * When applied to a color [r, g, b, a], the resulting color is computed as (after clamping) <br>
	 * R' = a*R + b*G + c*B + d*A + e;<br>
	 * G' = f*R + g*G + h*B + i*A + j;<br>
	 * B' = k*R + l*G + m*B + n*A + o;<br>
	 * A' = p*R + q*G + r*B + s*A + t;<br>
	 *
	 * @param matrix
	 * @return
	 */
	public static ImageData getImageDataWithColormatrixManipulation(float[] matrix, ImageData imgData) {

		int[] data = imgData.PixelColorArray;
		for (int i = 0; i < data.length; i++) {
			data[i] = HSV_Color.colorMatrixManipulation(data[i], matrix);
		}
		return imgData;
	}

	public void LoadTMS(String string) {
		try {
			TmsMap tmsMap = new TmsMap(string);
			if ((tmsMap.name == null) || (tmsMap.url == null)) {
				return;
			}
			tmsMaps.add(tmsMap);
			layers.add(new TmsLayer(Type.normal, tmsMap));
		} catch (Exception ex) {

		}

	}

	public void LoadBSH(String string) {
		try {
			BshLayer layer = new BshLayer(Type.normal, string);
			layers.add(layer);
		} catch (Exception ex) {

		}

	}

	private void getFiles(ArrayList<String> files, ArrayList<String> mapnames, String directory) {
		File dir = FileFactory.createFile(directory);
		String[] dirFiles = dir.list();
		if (dirFiles != null && dirFiles.length > 0) {
			for (String tmp : dirFiles) {
				String FilePath = directory + "/" + tmp;
				String ttt = tmp.toLowerCase();
				if (ttt.endsWith("pack") || ttt.endsWith("map") || ttt.endsWith("xml") || ttt.endsWith("bsh")) {
					if (!mapnames.contains(tmp)) {
						files.add(FilePath);
						mapnames.add(tmp);
						Log.debug(log, "add: " + tmp);
					}
				}
			}
		}
	}

	public void initMapPacks() {
		layers.clear();

		mayAddLayer = true;

		layers.addAll(DEFAULT_LAYER);

		if (LocatorSettings.UserMap1.getValue().length() > 0)
			layers.add(new Layer(MapType.ONLINE, Type.normal, "UserMap1", "", LocatorSettings.UserMap1.getValue()));
		if (LocatorSettings.UserMap2.getValue().length() > 0)
			layers.add(new Layer(MapType.ONLINE, Type.normal, "UserMap2", "", LocatorSettings.UserMap2.getValue()));

		ArrayList<String> files = new ArrayList<String>();
		ArrayList<String> mapnames = new ArrayList<String>();

		Log.debug(log, "dirOwnMaps = " + LocatorSettings.MapPackFolderLocal.getValue());
		getFiles(files, mapnames, LocatorSettings.MapPackFolderLocal.getValue());

		// if the Folder is changed, the user wants to use only this one
		// Log.debug(log, "dirDefaultMaps = " + LocatorSettings.MapPackFolder.getDefaultValue());
		// getFiles(files, mapnames, LocatorSettings.MapPackFolder.getDefaultValue());

		Log.debug(log, "dirGlobalMaps = " + LocatorSettings.MapPackFolder.getValue());
		getFiles(files, mapnames, LocatorSettings.MapPackFolder.getValue());

		if (files != null) {
			if (files.size() > 0) {
				for (String file : files) {
					if (FileIO.GetFileExtension(file).equalsIgnoreCase("pack")) {
						LoadMapPack(file);
					}
					if (FileIO.GetFileExtension(file).equalsIgnoreCase("map")) {

						java.io.File f = new java.io.File(FileFactory.createFile(file).getAbsolutePath());
						MapFile mapFile;
						try {
							mapFile = new MapFile(f);
						} catch (Exception e) {
							Log.err(log, "INIT MAPPACKS" + e.getMessage());
							continue;
						}

						//check type of mapsforge
						MapFileInfo info = mapFile.getMapFileInfo();
						MapType mapType = MapType.MAPSFORGE;
						if (info.comment != null && info.comment.contains("FZK")) {
							mapType = MapType.FREIZEITKARTE;
						}

						String Name = FileIO.GetFileNameWithoutExtension(file);
						Layer layer = new Layer(mapType, Type.normal, Name, Name, file);

						MultiMapDataStore md = new MultiMapDataStore(DataPolicy.RETURN_FIRST);

						md.addMapDataStore(mapFile, false, false);
						MapFileInfo mf = mapFile.getMapFileInfo();
						if (mf != null) {
							try {
								md.close();
								layer.boundingBox = mf.boundingBox;
								ManagerBase.Manager.layers.add(layer);
							} catch (Exception e) {
								Log.err(log, "Get boundingbox " + Name, e);
							}
						} else
							Log.err(log, "Problem open " + Name);
					}
					if (FileIO.GetFileExtension(file).equalsIgnoreCase("xml")) {
						ManagerBase.Manager.LoadTMS(file);
					}
					if (FileIO.GetFileExtension(file).equalsIgnoreCase("bsh")) {
						ManagerBase.Manager.LoadBSH(file);
					}
				}
			}
		}
		mayAddLayer = false;
	}

	public ArrayList<Layer> getLayers() {
		return layers;
	}

	// ##########################################################################
	// Mapsforge 0.6.0
	// ##########################################################################

	MultiMapDataStore mapDatabase[] = null;
	IDatabaseRenderer databaseRenderer[] = null;
	Bitmap tileBitmap = null;
	File mapFile = null;
	XmlRenderTheme renderTheme;
	public float textScale = 1;
	public static float DEFAULT_TEXT_SCALE = 1;

	public static final String INTERNAL_CAR_THEME = "internal-car-theme";
	private boolean invertToNightTheme; // not yet implemented?

	private RenderThemeFuture renderThemeFuture;

	public void setRenderTheme(String themePathAndName, boolean invert) {
		invertToNightTheme = invert;
		if (themePathAndName == null) {
			Log.debug(log, "Use RenderTheme CB_InternalRenderTheme.OSMARENDER");
			renderTheme = CB_InternalRenderTheme.OSMARENDER;
		} else {
			Log.debug(log, "Use RenderTheme " + themePathAndName);
			if (themePathAndName.equals(INTERNAL_CAR_THEME)) {
				renderTheme = CB_InternalRenderTheme.DAY_CAR_THEME;
			} else {
				try {
					File file = FileFactory.createFile(themePathAndName);
					if (file.exists()) {
						java.io.File themeFile = new java.io.File(file.getAbsolutePath());
						renderTheme = new ExternalRenderTheme(themeFile);
					} else {
						Log.err(log, themePathAndName + " not found!");
						renderTheme = CB_InternalRenderTheme.OSMARENDER;
					}
				} catch (FileNotFoundException e) {
					Log.err(log, "Load RenderTheme", "Error loading RenderTheme!", e);
					renderTheme = CB_InternalRenderTheme.OSMARENDER;
				}
			}
		}

		try {
			CB_RenderThemeHandler.getRenderTheme(getGraphicFactory(DISPLAY_MODEL.getScaleFactor()), DISPLAY_MODEL, renderTheme);
		} catch (Exception e) {
			Log.err(log, "RenderTheme: ", e);
			renderTheme = CB_InternalRenderTheme.OSMARENDER;
		}

		if (databaseRenderer == null) {
			databaseRenderer = new IDatabaseRenderer[PROCESSOR_COUNT];
		} else {
			for (int i = 0; i < PROCESSOR_COUNT; i++) {
				databaseRenderer[i] = null;
			}
		}

		this.renderThemeFuture = new RenderThemeFuture(this.getGraphicFactory(DISPLAY_MODEL.getScaleFactor()), this.renderTheme, this.DISPLAY_MODEL);
		new Thread(this.renderThemeFuture).start();

	}

	public TileGL getMapsforgePixMap(Layer layer, Descriptor desc, int ThreadIndex) {
		// Log.debug(log, "getTile " + layer.Name + " " + desc);
		// Mapsforge 0.4.0
		if ((mapDatabase == null)) {
			initMapDatabase(layer);
		}

		Tile tile = new Tile(desc.getX(), desc.getY(), (byte) desc.getZoom(), 256);

		RendererJob rendererJob = new RendererJob(tile, mapDatabase[ThreadIndex], this.renderThemeFuture, DISPLAY_MODEL, textScale, false, false);

		TileBasedLabelStore labelStore = null;

		if (databaseRenderer[ThreadIndex] == null) {
			GL_RenderType RENDERING_TYPE = LocatorSettings.MapsforgeRenderType.getEnumValue();

			switch (RENDERING_TYPE) {
			case Mapsforge:
				databaseRenderer[ThreadIndex] = new MF_DatabaseRenderer(mapDatabase[ThreadIndex], getGraphicFactory(DISPLAY_MODEL.getScaleFactor()), MF_DatabaseRenderer.firstLevelTileCache, labelStore, true, true);
				break;
			case Mixing:
				databaseRenderer[ThreadIndex] = new MixedDatabaseRenderer(mapDatabase[ThreadIndex], getGraphicFactory(DISPLAY_MODEL.getScaleFactor()), MF_DatabaseRenderer.firstLevelTileCache, labelStore, false, true);
				break;
			default:
				databaseRenderer[ThreadIndex] = new MF_DatabaseRenderer(mapDatabase[ThreadIndex], getGraphicFactory(DISPLAY_MODEL.getScaleFactor()), MF_DatabaseRenderer.firstLevelTileCache, labelStore, true, true);
				break;
			}
		}
		if (databaseRenderer[ThreadIndex] == null)
			return null;
		try {
			TileGL tileGL = databaseRenderer[ThreadIndex].execute(rendererJob);
			return tileGL;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void initMapDatabase(Layer layer) {
		mapFile = FileFactory.createFile(layer.Url);

		java.io.File file = new java.io.File(mapFile.getAbsolutePath());

		MapFile mapforgeMapFile = new MapFile(file);

		if (mapDatabase == null)
			mapDatabase = new MultiMapDataStore[PROCESSOR_COUNT];

		for (int i = 0; i < PROCESSOR_COUNT; i++) {
			if (mapDatabase[i] == null) {
				mapDatabase[i] = new MultiMapDataStore(DataPolicy.DEDUPLICATE);
			} else {
				mapDatabase[i].clearMapDataStore();
			}

			mapDatabase[i].addMapDataStore(mapforgeMapFile, false, false);

			//if the layer has more then one map, so add all files
			if (layer.hasAdidionalMaps()) {
				for (Layer addLayer : layer.getAdditionalMaps()) {
					File addMapFile = FileFactory.createFile(addLayer.Url);
					java.io.File addFile = new java.io.File(addMapFile.getAbsolutePath());
					MapFile addMapforgeMapFile = new MapFile(addFile);
					mapDatabase[i].addMapDataStore(addMapforgeMapFile, false, false);

				}
			}

		}

		Log.debug(log, "Open MapsForge Map: " + layer.Name);
	}

	public abstract GraphicFactory getGraphicFactory(float ScaleFactor);

}
