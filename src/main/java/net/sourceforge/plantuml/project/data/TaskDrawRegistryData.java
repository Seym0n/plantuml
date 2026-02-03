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

import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.plantuml.project.core.Task;
import net.sourceforge.plantuml.project.draw.TaskDraw;

/**
 * Value object containing the mapping from Task to TaskDraw.
 */
public class TaskDrawRegistryData {

	private final Map<Task, TaskDraw> draws = new LinkedHashMap<>();

	public TaskDraw getTaskDraw(Task task) {
		return draws.get(task);
	}

	// Mutators

	public void putTaskDraw(Task task, TaskDraw draw) {
		draws.put(task, draw);
	}

	// Internal access

	public Map<Task, TaskDraw> getDrawsMap() {
		return draws;
	}

}
