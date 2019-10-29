package CB_Locator.Map;

import CB_Locator.LocatorSettings;
import CB_UI_Base.settings.CB_UI_Base_Settings;
import CB_Utils.Log.Log;
import CB_Utils.Util.FileIO;
import CB_Utils.fileProvider.File;
import CB_Utils.fileProvider.FileFactory;
import com.badlogic.gdx.graphics.Pixmap;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.*;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;

import java.util.ArrayList;
import java.util.Set;

import static CB_Locator.LocatorBasePlatFormMethods.getMapsForgeGraphicFactory;

/**
 * MapsForge (Offline): getting tiles from a file in mapsforge format (one of these can be taken from Freizeitkarte)
 * you have to use theme for correct rendering
 */
public class MapsForgeLayer extends Layer {
    public static final String INTERNAL_THEME_DEFAULT = "Default";
    public static final String INTERNAL_THEME_OSMARENDER = "OsmaRender";
    public static final String INTERNAL_THEME_CAR = "Car";
    private static final float DEFAULT_TEXT_SCALE = 1;
    private static final String log = "MapsForgeLayer";
    public static DisplayModel displayModel;
    private static MultiMapDataStore[] multiMapDataStores;
    private static DatabaseRenderer[] databaseRenderers;
    private static RenderThemeFuture renderThemeFuture;
    private static int PROCESSOR_COUNT;
    private static boolean mustInitialize = true;
    private final ArrayList<Layer> additionalMapsforgeLayer;
    private final TileCache firstLevelTileCache; // perhaps static?
    private MapFile mapFile;
    private float textScale;
    private String pathAndName;
    private String mapsforgeThemesStyle;
    private String mapsforgeTheme;
    private boolean isSetRenderTheme;
    private int mDataStoreNumber;

    MapsForgeLayer(String pathAndName) {
        this.pathAndName = pathAndName;
        setMapFile(); // create mapFile from pathAndName
        MapFileInfo mapInfo = mapFile.getMapFileInfo();
        mapType = Layer.MapType.MAPSFORGE;
        if (mapInfo.comment != null && mapInfo.comment.contains("FZK")) {
            mapType = Layer.MapType.FREIZEITKARTE;
        }
        mLayerUsage = LayerUsage.normal;
        name = FileIO.getFileNameWithoutExtension(pathAndName);
        friendlyName = getName();
        url = "";
        storageType = Layer.StorageType.PNG;
        data = null;
        languages = mapFile.getMapLanguages();

        mDataStoreNumber = -1;
        firstLevelTileCache = new InMemoryTileCache(128);
        textScale = 1;

        additionalMapsforgeLayer = new ArrayList<>();

        mapsforgeThemesStyle = "";
        mapsforgeTheme = "";
        isSetRenderTheme = false;

        float restrictedScaleFactor = 1f;
        DisplayModel.setDeviceScaleFactor(restrictedScaleFactor);
        displayModel = new DisplayModel();

        if (mustInitialize) {
            // initialize these static things only once
            mustInitialize = false;
            PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
            multiMapDataStores = new MultiMapDataStore[PROCESSOR_COUNT];
            databaseRenderers = new DatabaseRenderer[PROCESSOR_COUNT];
            for (int i = 0; i < PROCESSOR_COUNT; i++)
                multiMapDataStores[i] = new MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE); // or DataPolicy.RETURN_FIRST
        }

    }

    @Override
    public void addAdditionalMap(Layer layer) {
        if (!this.isMapsForge() || !layer.isMapsForge())
            throw new RuntimeException("Can't add this Layer");
        MapsForgeLayer mapsforgeLayer = (MapsForgeLayer) layer;
        if (!additionalMapsforgeLayer.contains(mapsforgeLayer)) {
            additionalMapsforgeLayer.add(mapsforgeLayer);
            for (MultiMapDataStore mmds : multiMapDataStores) {
                mmds.addMapDataStore(mapsforgeLayer.getMapFile(), false, false);
            }
        }
    }

    @Override
    public void clearAdditionalMaps() {
        additionalMapsforgeLayer.clear();
        for (MultiMapDataStore mmds : multiMapDataStores) {
            mmds.clearMapDataStore();
            mmds.addMapDataStore(mapFile, false, false);
        }
    }

    @Override
    public boolean hasAdditionalMaps() {
        return additionalMapsforgeLayer.size() > 0;
    }

    @Override
    public String[] getAllLayerNames() {
        String[] ret = new String[additionalMapsforgeLayer.size() + 1];
        ret[0] = getName();
        int idx = 1;
        for (Layer additionalLayer : additionalMapsforgeLayer) {
            ret[idx] = additionalLayer.getName();
            idx++;
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Layer [");
        sb.append(this.getName());
        sb.append("] additional Layer:");

        if (additionalMapsforgeLayer == null || additionalMapsforgeLayer.isEmpty()) {
            sb.append("--");
        } else {
            for (Layer addLayer : additionalMapsforgeLayer) {
                sb.append(addLayer.getName()).append(", ");
            }
        }
        return sb.toString();
    }

    public void prepareLayer(boolean isCarMode) {
        try {
            for (int i = 0; i < PROCESSOR_COUNT; i++) {
                // Log.info(log, "multiMapDataStores[" + i + "].addMapDataStore: " + getName() + ": " + mapFile.getMapFileInfo().comment);
                multiMapDataStores[i].clearMapDataStore();
                multiMapDataStores[i].addMapDataStore(mapFile, false, false);
                for (Layer layer : additionalMapsforgeLayer) {
                    MapsForgeLayer mapsforgeLayer = (MapsForgeLayer) layer;
                    multiMapDataStores[i].addMapDataStore(mapsforgeLayer.getMapFile(), false, false);
                }
                // databaseRenderers[i] = new DatabaseRenderer(multiMapDataStores[i], getGraphicFactory(displayModel.getScaleFactor()), firstLevelTileCache, null, true, true);
                databaseRenderers[i] = new DatabaseRenderer(multiMapDataStores[i], getMapsForgeGraphicFactory(), firstLevelTileCache, null, true, true);
            }
            Log.info(log, "prepareLayer " + getName() + " : " + mapFile.getMapFileInfo().comment);
        } catch (Exception e) {
            Log.err(log, "ERROR with Open MapsForge Map: " + getName(), e);
        }

        initTheme(isCarMode);
    }

    @Override
    TileGL getTileGL(Descriptor desc) {
        // create bitmap from tile-definition
        try {
            Log.trace(log, "getTileGL: " + desc);
            Tile tile = new Tile(desc.getX(), desc.getY(), (byte) desc.getZoom(), 256);
            mDataStoreNumber = (mDataStoreNumber + 1) % PROCESSOR_COUNT;
            RendererJob rendererJob = new RendererJob(tile, multiMapDataStores[mDataStoreNumber], renderThemeFuture, displayModel, textScale, false, false);
            TileBitmap bitmap = databaseRenderers[mDataStoreNumber].executeJob(rendererJob);
            if (bitmap == null)
                return null;
            else {
                return new TileGL_Bmp(desc, bitmap, TileGL.TileState.Present, Pixmap.Format.RGB565);
            }
        } catch (Exception ex) {
            Log.err(log, "get mapsfore tile: ", ex);
            return null;
        }
    }

    private void setMapFile() {
        java.io.File file = new java.io.File(FileFactory.createFile(pathAndName).getAbsolutePath());
        if (getLanguages() == null) {
            mapFile = new MapFile(file);
        } else {
            String preferredLanguage = LocatorSettings.PreferredMapLanguage.getValue();
            if (preferredLanguage.length() > 0) {
                for (String la : getLanguages()) {
                    if (la.equals(preferredLanguage)) {
                        mapFile = new MapFile(file, preferredLanguage);
                        break;
                    }
                }
            }
            if (mapFile == null) {
                if (getLanguages().length > 0)
                    mapFile = new MapFile(file, getLanguages()[0]);
                else
                    mapFile = new MapFile(file);
            }
        }
    }

    boolean cacheTileToFile(Descriptor descriptor) {
        // don't want to cache for mapsforge
        return false;
    }

    public MapFile getMapFile() {
        return mapFile;
    }

    private void setRenderTheme(String theme, String themestyle) {
        if (isSetRenderTheme)
            if (theme.equals(mapsforgeTheme))
                if (themestyle.equals(mapsforgeThemesStyle))
                    return;
        mapsforgeThemesStyle = themestyle;
        mapsforgeTheme = theme;
        XmlRenderTheme renderTheme;
        if (mapsforgeTheme.length() == 0) {
            renderTheme = CB_InternalRenderTheme.DEFAULT;
        } else if (mapsforgeTheme.equals(INTERNAL_THEME_OSMARENDER)) {
            renderTheme = CB_InternalRenderTheme.OSMARENDER;
        } else if (mapsforgeTheme.equals(INTERNAL_THEME_CAR)) {
            renderTheme = CB_InternalRenderTheme.CAR;
        } else if (mapsforgeTheme.equals(INTERNAL_THEME_DEFAULT)) {
            renderTheme = CB_InternalRenderTheme.DEFAULT;
        } else {
            try {
                File file = FileFactory.createFile(mapsforgeTheme);
                if (file.exists()) {
                    java.io.File themeFile = new java.io.File(file.getAbsolutePath());
                    renderTheme = new ExternalRenderTheme(themeFile, new Xml_RenderThemeMenuCallback());
                } else {
                    Log.err(log, mapsforgeTheme + " not found!");
                    renderTheme = CB_InternalRenderTheme.DEFAULT;
                }
            } catch (Exception e) {
                Log.err(log, "Load RenderTheme", "Error loading RenderTheme!", e);
                renderTheme = CB_InternalRenderTheme.DEFAULT;
            }
        }

        try {
            // CB_RenderThemeHandler.getRenderTheme(getGraphicFactory(displayModel.getScaleFactor()), displayModel, renderTheme);
            RenderThemeHandler.getRenderTheme(getMapsForgeGraphicFactory(), displayModel, renderTheme);
        } catch (Exception e) {
            Log.err(log, "Error in checking RenderTheme " + mapsforgeTheme, e);
            renderTheme = CB_InternalRenderTheme.DEFAULT;
        }

        // renderThemeFuture = new RenderThemeFuture(getGraphicFactory(displayModel.getScaleFactor()), renderTheme, displayModel);
        renderThemeFuture = new RenderThemeFuture(getMapsForgeGraphicFactory(), renderTheme, displayModel);

        new Thread(renderThemeFuture).start();

        isSetRenderTheme = true;
    }

    public void initTheme(boolean carMode) {
        String mapsforgeThemesStyle;
        mapsforgeTheme = "";
        String path;
        if (carMode) {
            textScale = DEFAULT_TEXT_SCALE * 1.35f;
            mapsforgeTheme = INTERNAL_THEME_CAR;
            if (CB_UI_Base_Settings.nightMode.getValue()) {
                mapsforgeThemesStyle = LocatorSettings.MapsforgeCarNightStyle.getValue();
                path = LocatorSettings.MapsforgeCarNightTheme.getValue();
            } else {
                mapsforgeThemesStyle = LocatorSettings.MapsforgeCarDayStyle.getValue();
                path = LocatorSettings.MapsforgeCarDayTheme.getValue();
            }
        } else {
            textScale = DEFAULT_TEXT_SCALE * 2.0f;
            if (CB_UI_Base_Settings.nightMode.getValue()) {
                mapsforgeThemesStyle = LocatorSettings.MapsforgeNightStyle.getValue();
                path = LocatorSettings.MapsforgeNightTheme.getValue();
            } else {
                mapsforgeThemesStyle = LocatorSettings.MapsforgeDayStyle.getValue();
                path = LocatorSettings.MapsforgeDayTheme.getValue();
            }
        }
        if (path.length() > 0) {
            if (path.equals(INTERNAL_THEME_CAR) || path.equals(INTERNAL_THEME_DEFAULT) || path.equals(INTERNAL_THEME_OSMARENDER)) {
                mapsforgeTheme = path;
            } else if (FileIO.fileExists(path) && FileIO.getFileExtension(path).contains("xml")) {
                mapsforgeTheme = path;
            } else
                mapsforgeTheme = "";
        } else
            mapsforgeTheme = "";
        setRenderTheme(mapsforgeTheme, mapsforgeThemesStyle);
    }

    private class Xml_RenderThemeMenuCallback implements XmlRenderThemeMenuCallback {
        @Override
        public Set<String> getCategories(XmlRenderThemeStyleMenu style) {
            String ConfigStyle = mapsforgeThemesStyle;
            int StyleEnds = mapsforgeThemesStyle.indexOf("\t");
            String Style;
            if (StyleEnds > -1) {
                Style = mapsforgeThemesStyle.substring(0, StyleEnds);
            } else {
                Style = mapsforgeThemesStyle;
            }
            XmlRenderThemeStyleLayer selectedLayer = style.getLayer(Style);

            // now change the categories for this style
            if (selectedLayer == null) {
                return null;
            }
            Set<String> result = selectedLayer.getCategories();
            // add the categories from overlays that are enabled
            for (XmlRenderThemeStyleLayer overlay : selectedLayer.getOverlays()) {
                boolean overlayEnabled = overlay.isEnabled();
                int posInConfig = ConfigStyle.indexOf(overlay.getId());
                if (posInConfig > -1) {
                    overlayEnabled = ConfigStyle.substring(posInConfig - 1, posInConfig).equals("+");
                }
                if (overlayEnabled) {
                    result.addAll(overlay.getCategories());
                }
            }
            return result;
        }
    }

}
