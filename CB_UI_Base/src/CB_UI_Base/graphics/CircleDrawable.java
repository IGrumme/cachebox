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
package CB_UI_Base.graphics;

import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * @author Longri
 */
public class CircleDrawable extends PolygonDrawable
{

	final static float MIN_SEGMENTH_LENGTH = 10;
	final static int MIN_SEGMENTH_COUNT = 18;

	public SolidTextureRegion solidTextureRegion;

	final float RADIUS;

	final float X;
	final float Y;

	int SEGMENTE;

	PolygonRegion polygonRegion;

	public CircleDrawable(float x, float y, float radius, GL_Paint paint, float size, float size2)
	{
		super(paint, size, size2);

		RADIUS = radius;
		X = x;
		Y = y;

		createTriangles();
	}

	private void createTriangles()
	{
		// calculate segment count
		double alpha = (360 * MIN_SEGMENTH_LENGTH) / (MathUtils.PI2 * RADIUS);
		SEGMENTE = Math.max(MIN_SEGMENTH_COUNT, (int) (360 / alpha));

		// calculate theta step
		double thetaStep = (MathUtils.PI2 / SEGMENTE);

		if (PAINT.getGL_Style() == GL_Style.FILL)
		{
			// initialize arrays
			VERTICES = new float[(SEGMENTE + 1) * 2];
			TRIANGLES = new short[(SEGMENTE) * 3];

			int index = 0;

			// first point is the center point
			VERTICES[index++] = X;
			VERTICES[index++] = Y;

			int triangleIndex = 0;
			int verticeIdex = 1;
			boolean beginnTriangles = false;
			for (double i = 0; index < (SEGMENTE + 1) * 2; i += thetaStep)
			{
				VERTICES[index++] = (float) (X + RADIUS * Math.cos(i));
				VERTICES[index++] = (float) (Y + RADIUS * Math.sin(i));

				if (!beginnTriangles)
				{
					if (index % 6 == 0) beginnTriangles = true;
				}

				if (beginnTriangles)
				{
					TRIANGLES[triangleIndex++] = 0;
					TRIANGLES[triangleIndex++] = (short) verticeIdex++;
					TRIANGLES[triangleIndex++] = (short) verticeIdex;
				}

			}

			// last Triangle
			TRIANGLES[triangleIndex++] = 0;
			TRIANGLES[triangleIndex++] = (short) verticeIdex++;
			TRIANGLES[triangleIndex++] = (short) 1;

		}
		else
		{

			VERTICES = new float[(SEGMENTE) * 4];
			TRIANGLES = new short[(SEGMENTE) * 6];

			float halfStrokeWidth = (PAINT.strokeWidth) / 2;

			float radius1 = RADIUS - halfStrokeWidth;
			float radius2 = RADIUS + halfStrokeWidth;

			int index = 0;
			int triangleIndex = 0;
			int verticeIdex = 0;
			boolean beginnTriangles = false;
			for (float i = 0; index < (SEGMENTE * 4); i += thetaStep)
			{
				VERTICES[index++] = X + radius1 * MathUtils.cos(i);
				VERTICES[index++] = Y + radius1 * MathUtils.sin(i);
				VERTICES[index++] = X + radius2 * MathUtils.cos(i);
				VERTICES[index++] = Y + radius2 * MathUtils.sin(i);

				if (!beginnTriangles)
				{
					if (index % 8 == 0) beginnTriangles = true;
				}

				if (beginnTriangles)
				{
					TRIANGLES[triangleIndex++] = (short) verticeIdex++;
					TRIANGLES[triangleIndex++] = (short) verticeIdex++;
					TRIANGLES[triangleIndex++] = (short) verticeIdex--;

					TRIANGLES[triangleIndex++] = (short) verticeIdex++;
					TRIANGLES[triangleIndex++] = (short) verticeIdex++;
					TRIANGLES[triangleIndex++] = (short) verticeIdex--;
				}

			}

			// last two Triangles
			TRIANGLES[triangleIndex++] = (short) verticeIdex++;
			TRIANGLES[triangleIndex++] = (short) verticeIdex;
			TRIANGLES[triangleIndex++] = (short) 0;

			TRIANGLES[triangleIndex++] = (short) 0;
			TRIANGLES[triangleIndex++] = (short) 1;
			TRIANGLES[triangleIndex++] = (short) verticeIdex;
		}
	}
}
