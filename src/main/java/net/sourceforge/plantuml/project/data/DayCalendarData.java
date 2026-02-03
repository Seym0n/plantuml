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
package net.sourceforge.plantuml.project.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.project.OpenClose;
import net.sourceforge.plantuml.project.time.TimePoint;

/**
 * Value object containing day calendar information:
 * open/closed days, day colors, and day names.
 */
public class DayCalendarData {

	private final OpenClose openClose = new OpenClose();
	private final Map<TimePoint, String> nameDays = new HashMap<>();
	private final Map<TimePoint, HColor> colorDaysToday = new HashMap<>();
	private final Map<TimePoint, HColor> colorDaysInternal = new HashMap<>();
	private final Map<DayOfWeek, HColor> colorDaysOfWeek = new HashMap<>();

	public boolean isOpen(LocalDate day) {
		return openClose.getLoadAtDUMMY(day) > 0;
	}

	public boolean isOpen(TimePoint instant) {
		return openClose.getLoadAtDUMMY(instant.toDay()) > 0;
	}

	public HColor getDayColor(TimePoint day) {
		HColor color = colorDaysToday.get(day);
		if (color == null)
			color = colorDaysInternal.get(day);
		return color;
	}

	public HColor getDayOfWeekColor(DayOfWeek dayOfWeek) {
		return colorDaysOfWeek.get(dayOfWeek);
	}

	public String getDayName(TimePoint day) {
		return nameDays.get(day);
	}

	public OpenClose getOpenClose() {
		return openClose;
	}

	/**
	 * Returns a merged view of all day colors (today colors override internal colors).
	 */
	public Map<TimePoint, HColor> getColorDays() {
		final Map<TimePoint, HColor> result = new HashMap<>(colorDaysInternal);
		result.putAll(colorDaysToday);
		return Collections.unmodifiableMap(result);
	}

	public Map<TimePoint, String> getNameDays() {
		return Collections.unmodifiableMap(nameDays);
	}

	// Mutators

	public void putNameDay(TimePoint day, String name) {
		nameDays.put(day, name);
	}

	public void putColorDayToday(TimePoint day, HColor color) {
		colorDaysToday.put(day, color);
	}

	public void putColorDayInternal(TimePoint day, HColor color) {
		colorDaysInternal.put(day, color);
	}

	public void putColorDayOfWeek(DayOfWeek dow, HColor color) {
		colorDaysOfWeek.put(dow, color);
	}

}
