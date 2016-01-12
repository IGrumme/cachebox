/* 
 * Copyright (C) 2015 team-cachebox.de
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
package CB_UI_Base.GL_UI.Controls;

import CB_UI_Base.GL_UI.SpriteCacheBase;
import CB_UI_Base.GL_UI.Controls.Label.HAlignment;
import CB_UI_Base.Math.CB_RectF;
import CB_UI_Base.Math.UI_Size_Base;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;

public class RadioButton extends chkBox {

    private RadioGroup group;
    private final Image radioBack;
    private final Image radioSet;

    public RadioButton(String Name) {
	super(Name);
	radioBack = new Image(new CB_RectF(UI_Size_Base.that.getChkBoxSize()), name, false);
	radioBack.setDrawable(SpriteCacheBase.radioBack);
	this.addChild(radioBack);

	radioSet = new Image(new CB_RectF(UI_Size_Base.that.getChkBoxSize()), name, false);
	radioSet.setDrawable(SpriteCacheBase.radioOn);
	this.addChild(radioSet);
    }

    public void setRadioGroup(RadioGroup Group) {
	group = Group;
    }

    @Override
    protected void render(Batch batch) {
	if (lblTxt != null && lblTxt.getX() < radioBack.getMaxX()) {
	    lblTxt.setX(radioBack.getMaxX() + UI_Size_Base.that.getMargin());
	}

	if (isChk && !radioSet.isVisible()) {
	    radioSet.setVisible();
	} else if (!isChk && radioSet.isVisible()) {
	    radioSet.setVisible(false);
	}

	super.render(batch);
    }

    @Override
    public boolean click(int x, int y, int pointer, int button) {
	if (!isDisabled) {
	    if (!isChk || group == null) {
		isChk = !isChk;
		if (changeListener != null)
		    changeListener.onCheckedChanged(this, isChk);
		if (group != null)
		    group.aktivate(this);

	    }
	}
	return true;
    }

    @Override
    protected void Initial() {

    }

    @Override
    protected void SkinIsChanged() {
    }

    @Override
    public void setText(String Text, Color color) {
	setText(Text, null, color, HAlignment.LEFT);
    }

    @Override
    public void setText(String Text) {
	setText(Text, null, null, HAlignment.LEFT);
    }

    public void setText(String Text, HAlignment alignment) {
	setText(Text, null, null, alignment);
    }
}
