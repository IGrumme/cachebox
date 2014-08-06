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

package CB_Utils.Lists;

/**
 * The Stack class represents a last-in-first-out (LIFO) stack of objects. With option for max item Size.
 * 
 * @author Longri
 * @param <T>
 */
public class CB_Stack<T>
{

	private CB_List<T> items;
	private int maxItemSize = -1;

	public CB_Stack()
	{
		items = new CB_List<T>();
	}

	/**
	 * Add an item onto the last of this stack.
	 * 
	 * @param item
	 */
	public void add(T item)
	{
		items.add(item);
		checkMaxItemSize();
	}

	/**
	 * Removes the object at the top of this stack and returns that object as the value of this function.
	 * 
	 * @return
	 */
	public T get()
	{
		return items.remove(0);
	}

	public boolean contains(T value)
	{
		return items.contains(value);
	}

	public int getMaxItemSize()
	{
		return maxItemSize;
	}

	public void setMaxItemSize(int size)
	{
		maxItemSize = size;
		checkMaxItemSize();
	}

	/**
	 * Tests if this stack is empty.
	 * 
	 * @return
	 */
	public boolean empty()
	{
		return items.size == 0;
	}

	private void checkMaxItemSize()
	{
		if (maxItemSize < 1) return;
		if (items.size > maxItemSize)
		{
			int removeCount = items.size - maxItemSize;
			for (int i = 0; i < removeCount; i++)
			{
				items.remove(0);
			}
		}
	}
}
