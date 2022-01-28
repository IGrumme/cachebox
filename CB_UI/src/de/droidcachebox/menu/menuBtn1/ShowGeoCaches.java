package de.droidcachebox.menu.menuBtn1;

import static de.droidcachebox.gdx.activities.EditFilterSettings.applyFilter;
import static de.droidcachebox.menu.Action.ShowEditFilterSettings;
import static de.droidcachebox.menu.Action.ShowSearchDialog;

import com.badlogic.gdx.graphics.g2d.Sprite;

import de.droidcachebox.AbstractShowAction;
import de.droidcachebox.GlobalCore;
import de.droidcachebox.Platform;
import de.droidcachebox.core.CoreData;
import de.droidcachebox.core.FilterInstances;
import de.droidcachebox.core.FilterProperties;
import de.droidcachebox.database.CBDB;
import de.droidcachebox.database.CacheWithWP;
import de.droidcachebox.database.Database_Core;
import de.droidcachebox.gdx.CB_View_Base;
import de.droidcachebox.gdx.Sprites;
import de.droidcachebox.gdx.Sprites.IconName;
import de.droidcachebox.gdx.activities.EditCache;
import de.droidcachebox.gdx.controls.dialogs.ButtonDialog;
import de.droidcachebox.gdx.controls.dialogs.MsgBoxButton;
import de.droidcachebox.gdx.controls.dialogs.MsgBoxIcon;
import de.droidcachebox.gdx.main.Menu;
import de.droidcachebox.gdx.main.MenuItem;
import de.droidcachebox.locator.Locator;
import de.droidcachebox.menu.ViewManager;
import de.droidcachebox.menu.menuBtn1.contextmenus.ShowImportMenu;
import de.droidcachebox.menu.menuBtn1.contextmenus.ShowSelectDB;
import de.droidcachebox.menu.menuBtn1.executes.DeleteDialog;
import de.droidcachebox.menu.menuBtn1.executes.GeoCachesView;
import de.droidcachebox.settings.Settings;
import de.droidcachebox.translation.Translation;
import de.droidcachebox.utils.log.Log;

public class ShowGeoCaches extends AbstractShowAction {

    private EditCache editCache;
    private GeoCachesView geoCachesView;

    public ShowGeoCaches() {
        super("cacheList", "  (" + CBDB.getInstance().cacheList.size() + ")");
        editCache = null;
        geoCachesView = null;
    }

    @Override
    public void execute() {
        if (Platform.isGPSon())
            Platform.request_getLocationIfInBackground();
        if (geoCachesView == null)
            geoCachesView = new GeoCachesView();
        ViewManager.leftTab.showView(geoCachesView);
    }

    @Override
    public CB_View_Base getView() {
        return geoCachesView;
    }

    @Override
    public void viewIsHiding() {
        geoCachesView = null;
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Sprite getIcon() {
        return Sprites.getSprite(IconName.cacheListIcon.name());
    }

    @Override
    public boolean hasContextMenu() {
        return true;
    }

    @Override
    public Menu getContextMenu() {
        Menu cm = new Menu("CacheListViewTitle");

        String DBName = Settings.DatabaseName.getValue();
        try {
            int Pos = DBName.lastIndexOf(".");
            DBName = DBName.substring(0, Pos);
        } catch (Exception e) {
            DBName = "???";
        }

        MenuItem mi;
        cm.addMenuItem("ResortList", Sprites.getSprite(IconName.sortIcon.name()), () -> {
            if (!CBDB.getInstance().cacheList.resortAtWork) {
                synchronized (CBDB.getInstance().cacheList) {
                    Log.debug("ShowCacheList", "sort CacheList by Menu ResortList");
                    CacheWithWP nearestCacheWp = CBDB.getInstance().cacheList.resort(Locator.getInstance().getValidPosition(GlobalCore.getSelectedCache().getCoordinate()));
                    if (nearestCacheWp != null && nearestCacheWp.getCache() != null) {
                        GlobalCore.setSelectedWaypoint(nearestCacheWp.getCache(), nearestCacheWp.getWaypoint());
                        GlobalCore.setNearestCache(nearestCacheWp.getCache());
                    }
                    getGeoCachesView().setSelectedCacheVisible();
                }
            }
        });
        mi = cm.addMenuItem("setOrResetFilter", "", Sprites.getSprite(IconName.filter.name()), (v, x, y, pointer, button) -> {
            cm.close();
            boolean checked = ((MenuItem) v).isChecked();
            if (((MenuItem) v).isCheckboxClicked(x))
                checked = !checked;
            if (checked) {
                ShowEditFilterSettings.action.execute();
            } else {
                FilterInstances.setLastFilter(new FilterProperties());
                applyFilter(FilterInstances.getLastFilter());
            }
            return true;
        });
        mi.setCheckable(true);
        mi.setChecked(true);
        if (!FilterInstances.isLastFilterSet())
            mi.setCheckable(false);
        cm.addMenuItem("Search", Sprites.getSprite(IconName.lupe.name()), () -> {
            ShowSearchDialog.action.execute();
        });
        cm.addMenuItem("importExport", Sprites.getSprite(IconName.importIcon.name()), () -> new ShowImportMenu().execute());
        mi = cm.addMenuItem("setOrResetFavorites", "", Sprites.getSprite(IconName.favorit.name()), (v, x, y, pointer, button) -> {
            cm.close();
            boolean checked = ((MenuItem) v).isChecked();
            if (((MenuItem) v).isCheckboxClicked(x))
                checked = !checked;
            String msgText;
            if (checked) {
                msgText = "askSetFavorites";
            } else {
                msgText = "askResetFavorites";
            }
            final boolean finalChecked = checked;
            ButtonDialog bd = new ButtonDialog(Translation.get(msgText), Translation.get("Favorites"), MsgBoxButton.OKCancel, MsgBoxIcon.Question);
            bd.setButtonClickHandler((which, data) -> {
                if (which == ButtonDialog.BTN_LEFT_POSITIVE) {
                    CBDB.getInstance().beginTransaction();
                    Database_Core.Parameters args = new Database_Core.Parameters();
                    args.put("Favorit", finalChecked ? 1 : 0);
                    CBDB.getInstance().update("Caches", args, FilterInstances.getLastFilter().getSqlWhere(Settings.GcLogin.getValue()), null);
                    CBDB.getInstance().setTransactionSuccessful();
                    CBDB.getInstance().endTransaction();
                    ViewManager.reloadCacheList();
                    GlobalCore.checkSelectedCacheValid();
                }
                return true;
            });
            bd.show();
            return true;
        });
        mi.setCheckable(true);
        mi.setChecked(true); // default is to mark as Favorite
        cm.addMenuItem("manage", "  (" + DBName + ")", Sprites.getSprite(IconName.manageDb.name()), () -> new ShowSelectDB().execute());
        mi = cm.addMenuItem("AutoResort", null, () -> {
            GlobalCore.setAutoResort(!(GlobalCore.getAutoResort()));
            if (GlobalCore.getAutoResort()) {
                if (!CBDB.getInstance().cacheList.resortAtWork) {
                    synchronized (CBDB.getInstance().cacheList) {
                        Log.debug("ShowCacheList", "sort CacheList by Menu AutoResort");
                        CBDB.getInstance().cacheList.resort(Locator.getInstance().getValidPosition(GlobalCore.getSelectedCache().getCoordinate()));
                    }
                }
            }
        });
        mi.setCheckable(true);
        mi.setChecked(GlobalCore.getAutoResort());
        cm.addMenuItem("MI_NEW_CACHE", Sprites.getSprite(IconName.addCacheIcon.name()), () -> {
            if (editCache == null) editCache = new EditCache();
            if (editCache.isDisposed()) editCache = new EditCache();
            editCache.create();
        });
        cm.addMenuItem("DeleteCaches", Sprites.getSprite(IconName.DELETE.name()), () -> new DeleteDialog().show());
        cm.addMenuItem("ClearHistory", Sprites.getSprite("HISTORY"), this::clearHistory);

        return cm;
    }

    private void clearHistory() {
        CoreData.cacheHistory = "";
        if (FilterInstances.getLastFilter().isHistory) {
            applyFilter(FilterInstances.ALL);
            FilterInstances.setLastFilter(FilterInstances.ALL);
        }
    }

    public GeoCachesView getGeoCachesView() {
        if (geoCachesView == null) geoCachesView = new GeoCachesView();
        return geoCachesView;
    }

    public void setNameExtension(String newExtension) {
        titleExtension = newExtension;
    }
}
