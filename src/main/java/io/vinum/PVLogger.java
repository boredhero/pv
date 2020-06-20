/*
    Project Vinum - PVLogger.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package io.vinum;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

/**
 * Simple Log4J wrapper to help us log messages.
 */
public final class PVLogger {

	private static Logger logger;

	/* Make the constructor private to disable instantiation */
	private PVLogger() {
		throw new UnsupportedOperationException();
	}

	static void init(Logger logger) {

		if (PVLogger.logger == null) {
			PVLogger.logger = logger;
		} else {
			logger.warn("Trying to initialize mod logger more then once");
		}
	}

	@Contract(pure = true)
	public static Logger get() {
		return logger;
	}
	/*
	 * Short-hand methods to print logs to console.
	 */
	public static void info(String log) {
		logger.info(log);
	}
	public static void error(String log) {
		logger.error(log);
	}
	public static void error(String log, Object...args) {
		logger.printf(Level.ERROR, String.format(log, args));
	}
	public static void error(String log, Throwable t) {
		logger.error(log, t);
	}
	public static void warn(String log) {
		logger.warn(log);
	}
	public static void debug(String log) {
		logger.debug(log);
	}
	public static void debug(String format, Object...args) {
		logger.debug(String.format(format, args));
	}
	public static void debug(String log, Throwable t) {
		logger.debug(log, t);
	}
}
