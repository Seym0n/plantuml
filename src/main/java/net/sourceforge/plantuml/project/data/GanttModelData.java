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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.plantuml.project.GanttConstraint;
import net.sourceforge.plantuml.project.GanttModel;
import net.sourceforge.plantuml.project.core.Resource;
import net.sourceforge.plantuml.project.core.Task;
import net.sourceforge.plantuml.project.core.TaskCode;

/**
 * Value object containing the core domain data of a Gantt diagram:
 * tasks, resources, and constraints.
 */
public class GanttModelData implements GanttModel {

	private final List<GanttConstraint> constraints = new ArrayList<>();
	private final Map<TaskCode, Task> tasks = new LinkedHashMap<>();
	private final Map<String, Resource> resources = new LinkedHashMap<>();

	@Override
	public Collection<Task> getTasks() {
		return Collections.unmodifiableCollection(tasks.values());
	}

	@Override
	public Collection<Resource> getResources() {
		return Collections.unmodifiableCollection(resources.values());
	}

	@Override
	public Collection<GanttConstraint> getConstraints() {
		return Collections.unmodifiableCollection(constraints);
	}

	// Mutators for building the model

	public void addConstraint(GanttConstraint constraint) {
		constraints.add(constraint);
	}

	public void putTask(TaskCode code, Task task) {
		tasks.put(code, task);
	}

	public Task getTask(TaskCode code) {
		return tasks.get(code);
	}

	public void putResource(String name, Resource resource) {
		resources.put(name, resource);
	}

	public Resource getResource(String name) {
		return resources.get(name);
	}

	// Internal access for iteration

	Map<TaskCode, Task> getTasksMap() {
		return tasks;
	}

	Map<String, Resource> getResourcesMap() {
		return resources;
	}

}
