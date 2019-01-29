package CB_UI.GL_UI.Main.Actions;

import CB_UI.GL_UI.Main.TabMainView;
import CB_UI.GL_UI.Views.SolverView2;
import CB_UI_Base.GL_UI.CB_View_Base;
import CB_UI_Base.GL_UI.Main.Actions.CB_Action_ShowView;
import CB_UI_Base.GL_UI.Menu.Menu;
import CB_UI_Base.GL_UI.Menu.MenuID;
import CB_UI_Base.GL_UI.Sprites;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class CB_Action_ShowSolverView2 extends CB_Action_ShowView {

    private static CB_Action_ShowSolverView2 that;

    private CB_Action_ShowSolverView2() {
        super("Solver v2", MenuID.AID_SHOW_SOLVER2);
    }

    public static CB_Action_ShowSolverView2 getInstance() {
        if (that == null) that = new CB_Action_ShowSolverView2();
        return that;
    }

    @Override
    public void Execute() {
            TabMainView.leftTab.ShowView(SolverView2.getInstance());
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Sprite getIcon() {
        return Sprites.getSprite("solver-icon-2");
    }

    @Override
    public CB_View_Base getView() {
        return SolverView2.getInstance();
    }

    @Override
    public boolean hasContextMenu() {
        return true;
    }

    @Override
    public Menu getContextMenu() {
        return SolverView2.getInstance().getContextMenu();
    }
}
