/* 
 * Copyright (C) 2011-2014 team-cachebox.de
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
package CB_UI_Base.GL_UI;

import CB_UI_Base.GL_UI.Skin.SkinBase;
import CB_UI_Base.GL_UI.Skin.SkinSettings;

import com.badlogic.gdx.graphics.Color;

/**
 * Hold the loaded colors from Skin!
 * 
 * @author Longri
 */
public class COLOR
{
	private static Color day_fontColor;
	private static Color day_fontColorDisable;
	private static Color day_fontColorHighLight;
	private static Color day_fontColorLink;
	private static Color day_darknesColor;
	private static Color day_crossColor;
	private static Color day_MenuBackColor;

	private static Color night_fontColor;
	private static Color night_fontColorDisable;
	private static Color night_fontColorHighLight;
	private static Color night_fontColorLink;
	private static Color night_darknesColor;
	private static Color night_crossColor;
	private static Color night_MenuBackColor;

	private static SkinSettings cfg;

	public static void loadColors(SkinBase skin)
	{
		cfg = skin.getSettings();

		day_fontColor = getDayColor("font-color");
		day_fontColorDisable = getDayColor("font-color-disable");
		day_fontColorHighLight = getDayColor("font-color-highlight");
		day_fontColorLink = getDayColor("font-color-link");
		day_darknesColor = getDayColor("darknes");
		day_crossColor = getDayColor("cross");
		day_MenuBackColor = getDayColor("menu-back-color");

		night_fontColor = getNightColor("font-color");
		night_fontColorDisable = getNightColor("font-color-disable");
		night_fontColorHighLight = getNightColor("font-color-highlight");
		night_fontColorLink = getNightColor("font-color-link");
		night_darknesColor = getNightColor("darknes");
		night_crossColor = getNightColor("cross");
		night_MenuBackColor = getNightColor("menu-back-color");
	}

	private static Color getDayColor(String name)
	{

		Color ret = null;
		try
		{
			ret = SkinBase.getDaySkin().getColor(name);
		}
		catch (Exception e)
		{
		}

		if (ret == null) // use default from APK
		{
			ret = SkinBase.getDefaultDaySkin().getColor(name);
		}
		return ret;
	}

	private static Color getNightColor(String name)
	{

		Color ret = null;
		try
		{
			ret = SkinBase.getNightSkin().getColor(name);
		}
		catch (Exception e)
		{
		}

		if (ret == null) // use default from APK
		{
			ret = SkinBase.getDefaultNightSkin().getColor(name);
		}
		return ret;
	}

	public static Color getMenuBackColor()
	{
		return cfg.Nightmode ? night_MenuBackColor : day_MenuBackColor;
	}

	public static Color getFontColor()
	{
		return cfg.Nightmode ? night_fontColor : day_fontColor;
	}

	public static Color getDisableFontColor()
	{
		return cfg.Nightmode ? night_fontColorDisable : day_fontColorDisable;
	}

	public static Color getHighLightFontColor()
	{
		return cfg.Nightmode ? night_fontColorHighLight : day_fontColorHighLight;
	}

	public static Color getLinkFontColor()
	{
		return cfg.Nightmode ? night_fontColorLink : day_fontColorLink;
	}

	public static Color getDarknesColor()
	{
		return cfg.Nightmode ? night_darknesColor : day_darknesColor;
	}

	public static Color getCrossColor()
	{
		return cfg.Nightmode ? night_crossColor : day_crossColor;
	}
}
