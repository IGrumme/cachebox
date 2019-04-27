package CB_UI.GL_UI.Main.Actions;

import CB_Core.Database;
import CB_Core.Export.GpxSerializer;
import CB_Core.Export.GpxSerializer.ProgressListener;
import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI.Config;
import CB_UI.GL_UI.Activitys.*;
import CB_UI.GlobalCore;
import CB_UI_Base.Enums.WrapType;
import CB_UI_Base.Events.PlatformConnector;
import CB_UI_Base.Events.PlatformConnector.IgetFolderReturnListener;
import CB_UI_Base.GL_UI.CB_View_Base;
import CB_UI_Base.GL_UI.Controls.Dialogs.ProgressDialog;
import CB_UI_Base.GL_UI.Controls.Dialogs.ProgressDialog.ICancelListener;
import CB_UI_Base.GL_UI.Controls.Dialogs.StringInputBox;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBox;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBox.OnMsgBoxClickListener;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBoxIcon;
import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.GL_UI.Main.Actions.CB_Action_ShowView;
import CB_UI_Base.GL_UI.Menu.Menu;
import CB_UI_Base.GL_UI.Menu.MenuID;
import CB_UI_Base.GL_UI.Menu.MenuItem;
import CB_UI_Base.GL_UI.Sprites;
import CB_UI_Base.GL_UI.Sprites.IconName;
import CB_UI_Base.GL_UI.interfaces.RunnableReadyHandler;
import CB_Utils.Log.Log;
import CB_Utils.Util.FileIO;
import CB_Utils.fileProvider.File;
import CB_Utils.fileProvider.FileFactory;
import com.badlogic.gdx.graphics.g2d.Sprite;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CB_Action_ShowImportMenu extends CB_Action_ShowView {
    public static final int MI_IMPORT_CBS = 189;
    public static final int MI_IMPORT_GCV = 192;
    private static final int MI_CHK_STATE_API = 63;
    private static final int MI_IMPORT = 62;
    private static final int MI_IMPORT_GS_API_POSITION = 194;
    private static final int MI_IMPORT_GS_API_SEARCH = 195;
    private static final int MI_EXPORT_CBS = 190;
    private static final int MI_EXPORT_RUN = 196;
    private static final int MI_IMPORT_GSAK = 7;
    private static CB_Action_ShowImportMenu that;
    int actExportedCount = 0;
    private ProgressDialog pD;
    private boolean cancel = false;

    private CB_Action_ShowImportMenu() {
        super("ImportMenu", MenuID.AID_SHOW_IMPORT_MENU);
    }

    public static CB_Action_ShowImportMenu getInstance() {
        if (that == null) that = new CB_Action_ShowImportMenu();
        return that;
    }

    @Override
    public void Execute() {
        getContextMenu().Show();
    }

    @Override
    public CB_View_Base getView() {
        // don't return a view.
        // show menu direct.
        GL.that.RunOnGL(() -> Execute());

        return null;
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
        Menu icm = new Menu("CacheListShowImportMenu");

        icm.addOnItemClickListener((v, x, y, pointer, button) -> {
            switch (((MenuItem) v).getMenuItemId()) {
                case MI_CHK_STATE_API:
                    GL.that.postAsync(() -> {
                        // First check API-Key with visual Feedback
                        Log.debug("MI_CHK_STATE_API", "chkAPiLogInWithWaitDialog");
                        GlobalCore.chkAPiLogInWithWaitDialog(isAccessTokenInvalid -> {
                            Log.debug("checkReady", "isAccessTokenInvalid: " + isAccessTokenInvalid);
                            if (!isAccessTokenInvalid) {
                                TimerTask tt = new TimerTask() {
                                    @Override
                                    public void run() {
                                        GL.that.postAsync(() -> new Action_chkState().Execute());
                                    }
                                };
                                Timer t = new Timer();
                                t.schedule(tt, 100);
                            }
                        });
                    });
                    return true;
                case MI_IMPORT:
                    GL.that.postAsync(() -> new Import().show());
                    return true;
                case MI_IMPORT_GS_API_POSITION:
                    new SearchOverPosition().show();
                    return true;
                case MI_IMPORT_GS_API_SEARCH:
                    SearchOverNameOwnerGcCode.ShowInstanz();
                    return true;
                case MI_IMPORT_GCV:
                    new Import(MI_IMPORT_GCV).show();
                    return true;
                case MI_EXPORT_CBS:
                    new Import_CBServer().show();
                    return true;
                case MI_EXPORT_RUN:
                    StringInputBox.Show(WrapType.SINGLELINE, Translation.get("enterFileName"), ((MenuItem) v).getTitle(), FileIO.GetFileName(Config.gpxExportFileName.getValue()), new OnMsgBoxClickListener() {
                        @Override
                        public boolean onClick(int which, Object data) {
                            if (which == 1) {
                                final String FileName = StringInputBox.editText.getText();
                                GL.that.RunOnGL(() -> ExportgetFolderStep(FileName));
                            }
                            return true;
                        }
                    });
                    return true;
                case MI_IMPORT_GSAK:
                    new Import_GSAK().show();
                    return true;
            }
            return true;
        });
        icm.addItem(MI_CHK_STATE_API, "chkState"); // , Sprites.getSprite(IconName.dayGcLiveIcon.name())
        icm.addItem(MI_IMPORT, "moreImport");
        icm.addItem(MI_IMPORT_GS_API_POSITION, "importCachesOverPosition"); // "import"
        icm.addItem(MI_IMPORT_GS_API_SEARCH, "API_IMPORT_NAME_OWNER_CODE");
        icm.addItem(MI_IMPORT_GCV, "GCVoteRatings");
        icm.addItem(MI_IMPORT_GSAK, "GSAKMenuImport");
        icm.addDivider();
        icm.addItem(MI_EXPORT_RUN, "GPX_EXPORT");
        if (Config.CBS_IP.getValue().length() > 0) icm.addItem(MI_EXPORT_CBS, "ToCBServer");
        return icm;
    }

    private void ExportgetFolderStep(final String FileName) {
        PlatformConnector.getFolder(FileIO.GetDirectoryName(Config.gpxExportFileName.getValue()), Translation.get("selectExportFolder".hashCode()), Translation.get("select".hashCode()), new IgetFolderReturnListener() {
            @Override
            public void returnFolder(final String Path) {
                GL.that.RunOnGL(() -> ausgebenDatei(FileName, Path));
            }
        });
    }

    private void ausgebenDatei(final String FileName, String Path) {
        String exportPath = Path + "/" + FileName;
        PlatformConnector.addToMediaScannerList(exportPath);
        File exportFile = FileFactory.createFile(exportPath);
        Config.gpxExportFileName.setValue(exportFile.getPath());
        Config.AcceptChanges();

        // Delete File if exist
        if (exportFile.exists())
            try {
                exportFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }

        // Export all Caches from DB
        final ArrayList<String> allGeocodesForExport = Database.Data.cacheList.getGcCodes();

        final int count = allGeocodesForExport.size();
        actExportedCount = 0;
        // Show with Progress

        final GpxSerializer ser = new GpxSerializer();
        try {
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exportFile.getFileOutputStream(), "UTF-8"));

            pD = ProgressDialog.Show("export", new RunnableReadyHandler() {

                @Override
                public void run() {
                    try {
                        ser.writeGPX(allGeocodesForExport, writer, new ProgressListener() {
                            @Override
                            public void publishProgress(int countExported, String msg) {
                                actExportedCount = countExported;
                                if (pD != null) {
                                    int progress = (countExported * 100) / count;
                                    pD.setProgress("Export: " + countExported + "/" + count, msg, progress);
                                    if (pD.isCanceld())
                                        ser.cancel();
                                }
                            }
                        });
                    } catch (IOException ignored) {
                    }
                }

                @Override
                public boolean doCancel() {
                    return cancel;
                }

                @Override
                public void RunnableIsReady(boolean canceld) {
                    System.out.print("Export READY");
                    if (pD != null) {
                        pD.close();
                        pD.dispose();
                        pD = null;
                    }

                    if (canceld) {
                        MessageBox.show(Translation.get("exportedCanceld".hashCode(), String.valueOf(actExportedCount), String.valueOf(count)), Translation.get("export"), MessageBoxIcon.Stop);
                    } else {
                        MessageBox.show(Translation.get("exported".hashCode(), String.valueOf(actExportedCount)), Translation.get("export"), MessageBoxIcon.Information);
                    }

                }
            });

            pD.setCancelListener(new ICancelListener() {

                @Override
                public void isCanceled() {
                    cancel = true;
                    if (pD.isCanceld())
                        ser.cancel();
                }
            });
        } catch (IOException e) {

        }
    }

    private enum menuId {aktualisiereStatus, getFriends, downloadMap, importOverPosition, importByGcCode, importDiverse}

}
