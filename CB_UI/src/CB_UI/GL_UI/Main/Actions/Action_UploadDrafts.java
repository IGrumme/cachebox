package CB_UI.GL_UI.Main.Actions;

import CB_Core.Api.GroundspeakAPI;
import CB_Core.CB_Core_Settings;
import CB_Core.GCVote.GCVote;
import CB_Core.LogTypes;
import CB_Core.Types.Draft;
import CB_Core.Types.Drafts;
import CB_Core.Types.Drafts.LoadingType;
import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI.Config;
import CB_UI.GL_UI.Views.DraftsView;
import CB_UI_Base.GL_UI.Controls.Dialogs.ProgressDialog;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBox;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBoxButtons;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBoxIcon;
import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.GL_UI.Main.Actions.AbstractAction;
import CB_UI_Base.GL_UI.Menu.MenuID;
import CB_UI_Base.GL_UI.Sprites;
import CB_UI_Base.GL_UI.Sprites.IconName;
import CB_UI_Base.GL_UI.interfaces.RunnableReadyHandler;
import CB_Utils.Events.ProgresssChangedEventList;
import com.badlogic.gdx.graphics.g2d.Sprite;

import java.util.concurrent.atomic.AtomicBoolean;

public class Action_UploadDrafts extends AbstractAction {
    private static Action_UploadDrafts that;
    private Boolean ThreadCancel = false;
    private String UploadMeldung = "";
    private boolean API_Key_error = false;
    private ProgressDialog PD;

    private Action_UploadDrafts() {
        super("uploadDrafts", MenuID.AID_UPLOAD_DRAFT);
    }

    public static Action_UploadDrafts getInstance() {
        if (that == null) that = new Action_UploadDrafts();
        return that;
    }

    @Override
    public void Execute() {
        UploadDrafts();
    }

    @Override
    public Sprite getIcon() {
        return Sprites.getSprite(IconName.UPLOADFIELDNOTE.name());
    }

    private void UploadDrafts() {
        final AtomicBoolean cancel = new AtomicBoolean(false);

        final RunnableReadyHandler uploadDrafts = new RunnableReadyHandler() {

            @Override
            public void run() {
                ProgresssChangedEventList.Call("Upload", "", 0);

                Drafts drafts = new Drafts();

                drafts.loadDrafts("(Uploaded=0 or Uploaded is null)", LoadingType.Loadall);

                int count = 0;
                int anzahl = 0;
                for (Draft draft : drafts) {
                    if (!draft.uploaded)
                        anzahl++;
                }

                boolean sendGCVote = Config.GcVotePassword.getEncryptedValue().length() > 0;

                if (anzahl > 0) {
                    UploadMeldung = "";
                    API_Key_error = false;
                    for (Draft draft : drafts) {
                        if (cancel.get())
                            break;

                        if (draft.uploaded)
                            continue;
                        if (ThreadCancel) // wenn im ProgressDialog Cancel gedrückt
                            // wurde.
                            break;
                        // Progress status Melden
                        ProgresssChangedEventList.Call(draft.CacheName, (100 * count) / anzahl);

                        int result;

                        if (draft.isTbDraft) {
                            // there is no TB draft. we have to log direct
                            result = GroundspeakAPI.uploadTrackableLog(draft.TravelBugCode, draft.TrackingNumber, draft.gcCode, LogTypes.CB_LogType2GC(draft.type), draft.timestamp, draft.comment);
                        } else {
                            if (sendGCVote) {
                                if (draft.gc_Vote > 0)
                                    sendCacheVote(draft);
                            }
                            result = GroundspeakAPI.UploadDraftOrLog(draft.gcCode, draft.type.getGcLogTypeId(), draft.timestamp, draft.comment, draft.isDirectLog);
                        }

                        if (result == GroundspeakAPI.ERROR) {
                            GL.that.Toast(GroundspeakAPI.LastAPIError);
                            UploadMeldung += draft.gcCode + "\n" + GroundspeakAPI.LastAPIError + "\n";
                        } else {
                            // set draft as uploaded only when upload was working
                            draft.uploaded = true;
                            draft.UpdateDatabase();
                        }
                        count++;
                    }
                }
                PD.close();
            }

            @Override
            public boolean doCancel() {
                return cancel.get();
            }

            @Override
            public void RunnableIsReady(boolean canceld) {
                if (!canceld) {

                    if (!UploadMeldung.equals("")) {
                        if (!API_Key_error)
                            MessageBox.show(UploadMeldung, Translation.get("Error"), MessageBoxButtons.OK, MessageBoxIcon.Error, null);
                    } else {
                        MessageBox.show(Translation.get("uploadFinished"), Translation.get("uploadDrafts"), MessageBoxIcon.GC_Live);
                    }
                }
                DraftsView.getInstance().notifyDataSetChanged();
            }
        };

        // ProgressDialog Anzeigen und den Abarbeitungs Thread übergeben.

        GL.that.RunOnGL(() -> {
            PD = ProgressDialog.Show("uploadDrafts", uploadDrafts);
            PD.setCancelListener(() -> cancel.set(true));
        });

    }

    void sendCacheVote(Draft draft) {

        // Stimme abgeben
        try {
            if (!GCVote.sendVote(CB_Core_Settings.GcLogin.getValue(), CB_Core_Settings.GcVotePassword.getValue(), draft.gc_Vote, draft.CacheUrl, draft.gcCode)) {
                UploadMeldung += draft.gcCode + "\n" + "GC-Vote Error" + "\n";
            }
        } catch (Exception e) {
            UploadMeldung += draft.gcCode + "\n" + "GC-Vote Error" + "\n";
        }
    }

}
