package de.droidcachebox.menu.menuBtn2;

import static de.droidcachebox.core.GroundspeakAPI.OK;
import static de.droidcachebox.core.GroundspeakAPI.fetchGeoCacheLogs;

import com.badlogic.gdx.graphics.g2d.Sprite;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import de.droidcachebox.AbstractShowAction;
import de.droidcachebox.CacheSelectionChangedListeners;
import de.droidcachebox.GlobalCore;
import de.droidcachebox.core.GroundspeakAPI;
import de.droidcachebox.database.CBDB;
import de.droidcachebox.database.LogsTableDAO;
import de.droidcachebox.dataclasses.LogEntry;
import de.droidcachebox.gdx.CB_View_Base;
import de.droidcachebox.gdx.GL;
import de.droidcachebox.gdx.Sprites;
import de.droidcachebox.gdx.Sprites.IconName;
import de.droidcachebox.gdx.controls.animation.DownloadAnimation;
import de.droidcachebox.gdx.controls.dialogs.CancelWaitDialog;
import de.droidcachebox.gdx.controls.messagebox.MsgBox;
import de.droidcachebox.gdx.controls.messagebox.MsgBoxButton;
import de.droidcachebox.gdx.controls.messagebox.MsgBoxIcon;
import de.droidcachebox.gdx.main.Menu;
import de.droidcachebox.menu.ViewManager;
import de.droidcachebox.menu.menuBtn2.executes.Logs;
import de.droidcachebox.menu.menuBtn2.executes.Spoiler;
import de.droidcachebox.settings.Settings;
import de.droidcachebox.translation.Translation;
import de.droidcachebox.utils.RunAndReady;

public class ShowLogs extends AbstractShowAction {

    private static ShowLogs that;
    private final int result = 0;

    private ShowLogs() {
        super("ShowLogs");
        // createContextMenu();  see getContextMenu
    }

    public static ShowLogs getInstance() {
        if (that == null) that = new ShowLogs();
        return that;
    }

    @Override
    public void execute() {
        GlobalCore.filterLogsOfFriends = false; // Reset Filter by Friends when opening LogListView
        ViewManager.leftTab.showView(Logs.getInstance());
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Sprite getIcon() {
        return Sprites.getSprite(IconName.listIcon.name());
    }

    @Override
    public CB_View_Base getView() {
        return Logs.getInstance();
    }

    @Override
    public boolean hasContextMenu() {
        return true;
    }

    @Override
    public Menu getContextMenu() {
        // todo why are the clickHandlers of the items gone on subsequent calls? temp solution createContextMenu() again
        // has to do with the disposing of the compoundMenu in CB_Button after the Show
        return createContextMenu();
    }

    private Menu createContextMenu() {
        Menu contextMenu = new Menu("LogListViewContextMenuTitle");
        contextMenu.addMenuItem("ReloadLogs", Sprites.getSprite(IconName.downloadLogs.name()), () -> loadLogs(true));
        if (Settings.friends.getValue().length() > 0) {
            contextMenu.addMenuItem("LoadLogsOfFriends", Sprites.getSprite(IconName.downloadFriendsLogs.name()), () -> loadLogs(false));
            contextMenu.addCheckableMenuItem("FilterLogsOfFriends", Sprites.getSprite(IconName.friendsLogs.name()), GlobalCore.filterLogsOfFriends, () -> {
                GlobalCore.filterLogsOfFriends = !GlobalCore.filterLogsOfFriends;
                Logs.getInstance().resetIsInitialized();
            });
        }
        contextMenu.addMenuItem("ImportFriends", Sprites.getSprite(Sprites.IconName.friends.name()), this::getFriends);

        contextMenu.addMenuItem("LoadLogImages", Sprites.getSprite(IconName.downloadLogImages.name()),
                () -> ShowSpoiler.getInstance().importSpoiler(true, isCanceled -> {
                    // do after import
                    if (!isCanceled) {
                        if (GlobalCore.isSetSelectedCache()) {
                            GlobalCore.getSelectedCache().loadSpoilerRessources();
                            Spoiler.getInstance().ForceReload();
                        }
                    }
                }));
        return contextMenu;
    }

    private void loadLogs(boolean loadAllLogs) {

        final AtomicBoolean isCanceled = new AtomicBoolean();
        isCanceled.set(false);

        /*
        do after CancelWaitDialog
        public void ready(boolean canceled) {
            String sCanceled = canceled ? Translation.get("isCanceled") + br : "";
            pd.close();
            if (result != -1) {
                synchronized (CBDB.getInstance().cacheList) {
                    MsgBox.show(sCanceled + Translation.get("LogsLoaded") + " " + ChangedCount, Translation.get("LoadLogs"), MsgBoxIcon.None);
                }

            }
        }

         */

        GL.that.postAsync(() -> GlobalCore.chkAPiLogInWithWaitDialog(MemberType -> {
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    GL.that.postAsync(() -> {
                        new CancelWaitDialog(Translation.get("LoadLogs"), new DownloadAnimation(), new RunAndReady() {
                            @Override
                            public void ready(boolean isCanceled) {

                            }

                            @Override
                            public void run() {
                                ArrayList<LogEntry> logList;
                                logList = fetchGeoCacheLogs(GlobalCore.getSelectedCache(), loadAllLogs, isCanceled::get);
                                if (logList.size() > 0) {
                                    CBDB.getInstance().beginTransaction();
                                    if (loadAllLogs)
                                        LogsTableDAO.getInstance().deleteLogs(GlobalCore.getSelectedCache().generatedId);
                                    for (LogEntry logEntry : logList) {
                                        LogsTableDAO.getInstance().WriteLogEntry(logEntry);
                                    }
                                    CBDB.getInstance().setTransactionSuccessful();
                                    CBDB.getInstance().endTransaction();
                                    // update LogListView
                                    Logs.getInstance().resetIsInitialized();
                                    // for update slider, ?, ?, ? with latest logs
                                    CacheSelectionChangedListeners.getInstance().fireEvent(GlobalCore.getSelectedCache(), GlobalCore.getSelectedWayPoint());
                                }
                            }
                        }).show();
                    });
                }
            };
            Timer t = new Timer();
            t.schedule(tt, 100);
        }));
    }

    private void getFriends() {
        GL.that.postAsync(() -> {
            String friends = GroundspeakAPI.fetchFriends();
            if (GroundspeakAPI.APIError == OK) {
                Settings.friends.setValue(friends);
                Settings.getInstance().acceptChanges();
                MsgBox.show(Translation.get("ok") + ":\n" + friends, Translation.get("Friends"), MsgBoxButton.OK, MsgBoxIcon.Information, null);
            } else {
                MsgBox.show(GroundspeakAPI.LastAPIError, Translation.get("Friends"), MsgBoxButton.OK, MsgBoxIcon.Information, null);
            }
        });
    }

}
