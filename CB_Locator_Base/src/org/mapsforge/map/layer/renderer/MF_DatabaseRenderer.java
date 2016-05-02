/* 
 * Copyright (C) 2014 team-cachebox.de
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
package org.mapsforge.map.layer.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;

import com.badlogic.gdx.graphics.Pixmap.Format;

import CB_Locator.Map.Descriptor;
import CB_Locator.Map.TileGL;
import CB_Locator.Map.TileGL.TileState;
import CB_Locator.Map.TileGL_Bmp;
import CB_UI_Base.graphics.extendedIntrefaces.ext_Bitmap;

/**
 * @author Longri
 */
public class MF_DatabaseRenderer extends DatabaseRenderer implements IDatabaseRenderer {

	private double tileLatLon_0_x, tileLatLon_0_y, tileLatLon_1_x, tileLatLon_1_y;
	private double divLon, divLat;

	static TileCache firstLevelTileCache = new InMemoryTileCache(128);

	public MF_DatabaseRenderer(MapDataStore mapDatabase, GraphicFactory graphicFactory, TileBasedLabelStore labelStore) {
		super(mapDatabase, graphicFactory, firstLevelTileCache);

	}

	@Override
	public TileGL execute(RendererJob rendererJob) {

		// Fixme direct Buffer swap
		/*
		 * If the goal is to convert an Android Bitmap to a libgdx Texture, you don't need to use Pixmap at all. You can do it directly with
		 * the help of simple OpenGL and Android GLUtils. Try the followings; it is 100x faster than your solution. I assume that you are
		 * not in the rendering thread (you should not most likely). If you are, you don't need to call postRunnable().
		 * 
		 * Gdx.app.postRunnable(new Runnable() {
		 * 
		 * @Override public void run() { Texture tex = new Texture(bitmap.getWidth(), bitmap.getHeight(), Format.RGBA8888);
		 * GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.getTextureObjectHandle()); GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		 * GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); bitmap.recycle(); // now you have the texture to do whatever you want } });
		 */

		int tileSize = rendererJob.displayModel.getTileSize();
		Tile tile = rendererJob.tile;
		tileLatLon_0_x = MercatorProjection.tileXToLongitude(tile.tileX, tile.zoomLevel);
		tileLatLon_0_y = MercatorProjection.tileYToLatitude(tile.tileY, tile.zoomLevel);
		tileLatLon_1_x = MercatorProjection.tileXToLongitude(tile.tileX + 1, tile.zoomLevel);
		tileLatLon_1_y = MercatorProjection.tileYToLatitude(tile.tileY + 1, tile.zoomLevel);

		divLon = (tileLatLon_0_x - tileLatLon_1_x) / tileSize;
		divLat = (tileLatLon_0_y - tileLatLon_1_y) / tileSize;

		TileBitmap bmp = executeJob(rendererJob);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			bmp.compress(baos);
			byte[] b = baos.toByteArray();

			Descriptor desc = new Descriptor(rendererJob.tile.tileX, rendererJob.tile.tileY, rendererJob.tile.zoomLevel, false);

			TileGL_Bmp bmpTile = new TileGL_Bmp(desc, b, TileState.Present, Format.RGB565);

			((ext_Bitmap) bmp).recycle();

			return bmpTile;
		} catch (IOException e) {

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Converts the given LatLong into XY coordinates on the current object.
	 * 
	 * @param latLong
	 *            the LatLong to convert.
	 * @return the XY coordinates on the current object.
	 */
	private Point scaleLatLong(LatLong latLong, int tileSize) {
		double pixelX = (tileLatLon_0_x - latLong.getLongitude()) / divLon;
		double pixelY = (tileLatLon_0_y - latLong.getLatitude()) / divLat;

		return new Point((float) pixelX, (float) pixelY);
	}

}
