package CB_UI.GL_UI.Main.Actions;

import CB_UI.GL_UI.Controls.PopUps.SearchDialog;
import CB_UI.GL_UI.Main.TabMainView;
import CB_UI.GL_UI.Views.CacheListView;
import CB_UI_Base.GL_UI.Main.Actions.CB_Action;
import CB_UI_Base.GL_UI.Menu.MenuID;
import CB_UI_Base.GL_UI.Sprites;
import CB_UI_Base.GL_UI.Sprites.IconName;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class CB_Action_Show_Search extends CB_Action {

    private static CB_Action_Show_Search that;

    private CB_Action_Show_Search() {
        super("Search", MenuID.AID_SEARCH);
    }

    public static CB_Action_Show_Search getInstance() {
        if (that == null) that = new CB_Action_Show_Search();
        return that;
    }

    @Override
    public void Execute() {

        if (!CacheListView.getInstance().isVisible()) {
            CB_Action_ShowCacheList.getInstance().Execute();
        }

        if (SearchDialog.that == null) {
            new SearchDialog();
        }
        SearchDialog.that.showNotCloseAutomaticly();
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Sprite getIcon() {
        return Sprites.getSprite(IconName.lupe.name());
    }
}
