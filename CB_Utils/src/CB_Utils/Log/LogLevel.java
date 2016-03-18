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
package CB_Utils.Log;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Defines the set of levels recognized by logback-classic, that is: <br>
 * {@link #OFF} = Integer.MAX_VALUE <br>
 * {@link #ERROR} = 40000 <br>
 * {@link #WARN} = 30000 <br>
 * {@link #INFO} =20000 <br>
 * {@link #DEBUG} = 10000 <br>
 * {@link #TRACE} = 5000 <br>
 * {@link #ALL} = Integer.MIN_VALUE <br>
 * <p/>
 * Additional LogLevl is <br>
 * {@link #GPS_Trace} = 7000
 * 
 * @author Longri 2014
 */
public enum LogLevel {
	OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL;

	final static org.slf4j.Logger log = LoggerFactory.getLogger(LogLevel.class);
	private static LogLevel act = OFF;

	/**
	 * Returns the int value of this {@link #LogLevel}
	 * 
	 * @return
	 */
	public int toInt() {
		switch (this) {
		case ALL:
			return Level.ALL_INT;
		case DEBUG:
			return Level.DEBUG_INT;
		case ERROR:
			return Level.ERROR_INT;
		case INFO:
			return Level.INFO_INT;
		case OFF:
			return Level.OFF_INT;
		case TRACE:
			return Level.TRACE_INT;
		case WARN:
			return Level.WARN_INT;
		default:
			return Level.OFF_INT;
		}
	}

	/**
	 * Actual logging level
	 * 
	 * @param level
	 *            {@link #LogLevel}
	 */
	static void setLogLevel(LogLevel level) {
		Level actlevel = Level.toLevel(level.toInt(), Level.OFF);
		if (actlevel == Level.OFF) {
			act = LogLevel.OFF;
		} else {
			act = level;
		}

		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		root.setLevel(Level.ALL);

		Log.info(log, "Set LogLevel to:" + act.toString());
		root.setLevel(actlevel);
	}

	static LogLevel getLogLevel() {
		return act;
	}

	/**
	 * Returns {@link true} if the int value of the given {@link #LogLevel} higher or equals the actual {@link #LogLevel}<br>
	 * Otherwise returns false;
	 * 
	 * @param level
	 *            {@link #LogLevel}
	 * @return
	 */
	public static boolean isLogLevel(LogLevel level) {
		return act.toInt() <= level.toInt();
	}
}
