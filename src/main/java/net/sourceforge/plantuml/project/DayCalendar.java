/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2024, Arnaud Roques
 *
 * Project Info:  https://plantuml.com
 *
 * If you like this project or if you find it useful, you can support us at:
 *
 * https://plantuml.com/patreon (only 1$ per month!)
 * https://plantuml.com/paypal
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 *
 */
package net.sourceforge.plantuml.project;

import java.time.DayOfWeek;
import java.time.LocalDate;

import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.project.time.TimePoint;

/**
 * Provides information about day status (open/closed),
 * day colors, and custom day names.
 */
public interface DayCalendar {

	/**
	 * Returns true if the given day is open (working day).
	 */
	boolean isOpen(LocalDate day);

	/**
	 * Returns true if the given time point falls on an open day.
	 */
	boolean isOpen(TimePoint instant);

	/**
	 * Returns the background color for a specific day, or null if default.
	 */
	HColor getDayColor(TimePoint day);

	/**
	 * Returns the background color for a day of week, or null if default.
	 */
	HColor getDayOfWeekColor(DayOfWeek dayOfWeek);

	/**
	 * Returns the custom name for a specific day, or null if none.
	 */
	String getDayName(TimePoint day);

	/**
	 * Returns the OpenClose instance for detailed day status queries.
	 */
	OpenClose getOpenClose();

}
