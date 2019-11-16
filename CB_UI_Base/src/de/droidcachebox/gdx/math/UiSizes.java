/*
 * Copyright (C) 2011 team-cachebox.de
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

package de.droidcachebox.gdx.math;

import de.droidcachebox.CB_UI_Base_Settings;

/**
 * Enthält die Größen einzelner Controls
 *
 * @author Longri
 */
public class UiSizes {

    private static UiSizes uiSizes;
    private int ButtonHeight;
    private int WideButtonWidth;
    private int scaledFontSize_normal;
    private int iconSize;
    private int windowWidth;
    private int windowHeight;
    private int scaledFontSize_big;
    private int scaledFontSize_btn;
    private int ScaledFontSize_small;
    private float scale;
    private int margin;
    int RefWidth;
    private int mClickToleranz;
    private Size QuickButtonList;
    private int scaledIconSize;
    private int CornerSize;
    private int infoSliderHeight;
    private int spaceWidth;
    private int tabWidth;
    private int halfCornerSize;
    private Size CacheListItemSize;
    private CB_Rect CacheListDrawRec;
    private int arrowScaleList;
    private int TB_icon_Size;
    private CB_RectF ButtonRectF;
    private boolean isInitialized;

    private UiSizes() {
        isInitialized = false;
    }

    public static UiSizes getInstance() {
        if (uiSizes == null) uiSizes = new UiSizes();
        return uiSizes;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public Size initialize(DevicesSizes devicesSizes) {
        this.windowWidth = devicesSizes.Window.width;
        this.windowHeight = devicesSizes.Window.height;
        this.scale = devicesSizes.Density; // res.getDisplayMetrics().density;

        mClickToleranz = (int) (17 * scale);

        margin = (int) (10 * scale);

        float NormalTextSize = CB_UI_Base_Settings.FONT_SIZE_NORMAL.getValue() * 3.2f;

        ButtonHeight = (int) (NormalTextSize * scale);
        WideButtonWidth = (windowWidth - 4 * margin) / 3;

        RefWidth = windowWidth;

        scaledFontSize_normal = (int) (10 * scale);
        scaledFontSize_big = (int) (scaledFontSize_normal * 1.1);
        ScaledFontSize_small = (int) (scaledFontSize_normal * 0.9);
        scaledFontSize_btn = (int) (11 * scale);

        iconSize = (int) (10 * scale);

        int quickButtonRef = 320;
        QuickButtonList = new Size((int) (quickButtonRef * scale - (13.3333f * scale)), (int) (((quickButtonRef * scale) / 5) - 4 * scale));

        scaledIconSize = (int) (10 * scale);

        CornerSize = (int) (10 * scale);
        infoSliderHeight = (int) (30 * scale);

        spaceWidth = (int) (scaledFontSize_normal * 0.9);
        tabWidth = (int) (scaledFontSize_normal * 0.6);
        halfCornerSize = CornerSize / 2;

        float ItemHeight = devicesSizes.Density * 63;

        CacheListItemSize = new Size(RefWidth, (int) ItemHeight);
        CacheListDrawRec = CacheListItemSize.getBounds(5, 2, -5, -2);

        arrowScaleList = (int) (10 * scale);
        TB_icon_Size = (int) (10 * scale);
        ButtonRectF = new CB_RectF(0, 0, ButtonHeight);

        isInitialized = true;
        return new Size(windowWidth, windowHeight);
    }

    public int getMargin() {
        return margin;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getButtonHeight() {
        return ButtonHeight;
    }

    public int getButtonWidthWide() {
        return WideButtonWidth;
    }

    public SizeF getChkBoxSize() {
        float h = ButtonHeight * 0.88f;
        return new SizeF(h, h);
    }

    public int getScaledFontSize() {
        return scaledFontSize_normal;
    }

    public int getScaledFontSize_btn() {
        return scaledFontSize_btn;
    }

    public int getScaledFontSize_big() {
        return scaledFontSize_big;
    }

    public int getScaledFontSize_small() {
        return ScaledFontSize_small;
    }

    public int getIconSize() {
        return iconSize;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getSmallestWidth() {
        return Math.min(windowHeight, windowWidth);
    }

    public int getClickToleranz() {
        return mClickToleranz;
    }

    public int getTbIconSize() {
        return TB_icon_Size;
    }

    public int getArrowScaleList() {
        return arrowScaleList;
    }

    public int getQuickButtonListHeight() {
        return QuickButtonList.height;
    }

    public int getInfoSliderHeight() {
        return infoSliderHeight;
    }

    public Size getCacheListItemSize() {
        return CacheListItemSize;
    }

    public CB_Rect getCacheListItemRec() {
        return CacheListDrawRec;
    }

    public int getCornerSize() {
        return CornerSize;
    }

    public int getScaledIconSize() {
        return scaledIconSize;
    }

    public int getSpaceWidth() {
        return spaceWidth;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public int getHalfCornerSize() {
        return halfCornerSize;
    }

    public CB_RectF getButtonRectF() {
        return ButtonRectF;
    }

}
