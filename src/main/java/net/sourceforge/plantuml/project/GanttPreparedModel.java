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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.color.HColorSet;
import net.sourceforge.plantuml.klimt.drawing.UGraphic;
import net.sourceforge.plantuml.klimt.font.StringBounder;
import net.sourceforge.plantuml.klimt.font.UFont;
import net.sourceforge.plantuml.project.core.PrintScale;
import net.sourceforge.plantuml.project.core.Resource;
import net.sourceforge.plantuml.project.core.Task;
import net.sourceforge.plantuml.project.core.TaskGroup;
import net.sourceforge.plantuml.project.core.TaskImpl;
import net.sourceforge.plantuml.project.core.TaskSeparator;
import net.sourceforge.plantuml.project.data.DayCalendarData;
import net.sourceforge.plantuml.project.data.DisplayConfigData;
import net.sourceforge.plantuml.project.data.GanttModelData;
import net.sourceforge.plantuml.project.data.TaskDrawRegistryData;
import net.sourceforge.plantuml.project.data.TimeBoundsData;
import net.sourceforge.plantuml.project.data.TimeScaleConfigData;
import net.sourceforge.plantuml.project.data.TimelineStyleData;
import net.sourceforge.plantuml.project.data.VerticalSeparatorsData;
import net.sourceforge.plantuml.project.data.WeekConfigData;
import net.sourceforge.plantuml.project.draw.FingerPrint;
import net.sourceforge.plantuml.project.draw.ResourceDraw;
import net.sourceforge.plantuml.project.draw.ResourceDrawNumbers;
import net.sourceforge.plantuml.project.draw.TaskDraw;
import net.sourceforge.plantuml.project.draw.TaskDrawDiamond;
import net.sourceforge.plantuml.project.draw.TaskDrawGroup;
import net.sourceforge.plantuml.project.draw.TaskDrawRegular;
import net.sourceforge.plantuml.project.draw.TaskDrawSeparator;
import net.sourceforge.plantuml.project.draw.header.TimeHeader;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderDaily;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderMonthly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderQuarterly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderWeekly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderYearly;
import net.sourceforge.plantuml.project.ngm.math.PiecewiseConstant;
import net.sourceforge.plantuml.project.time.TimePoint;
import net.sourceforge.plantuml.project.timescale.TimeScale;
import net.sourceforge.plantuml.project.timescale.TimeScaleCompressed;
import net.sourceforge.plantuml.project.timescale.TimeScaleDaily;
import net.sourceforge.plantuml.project.timescale.TimeScaleDailyHideClosed;
import net.sourceforge.plantuml.project.timescale.TimeScaleWink;
import net.sourceforge.plantuml.real.Real;
import net.sourceforge.plantuml.real.RealOrigin;
import net.sourceforge.plantuml.real.RealUtils;
import net.sourceforge.plantuml.skin.Pragma;
import net.sourceforge.plantuml.style.ISkinParam;
import net.sourceforge.plantuml.style.SName;

public class GanttPreparedModel implements ToTaskDraw {

	// ========================================================================
	// Value objects
	// ========================================================================
	private final GanttModelData modelData = new GanttModelData();

	public GanttModelData getModelData() {
		return modelData;
	}

	private final TimeBoundsData timeBounds = new TimeBoundsData();

	public TimeBoundsData getTimeBounds() {
		return timeBounds;
	}

	private final TimeScaleConfigData scaleConfig = new TimeScaleConfigData();

	public TimeScaleConfigData getScaleConfig() {
		return scaleConfig;
	}

	private final WeekConfigData weekConfig = new WeekConfigData();

	public WeekConfigData getWeekConfig() {
		return weekConfig;
	}

	private final DayCalendarData dayCalendar = new DayCalendarData();

	public DayCalendarData getDayCalendar() {
		return dayCalendar;
	}

	private final DisplayConfigData displayConfig = new DisplayConfigData();

	public DisplayConfigData getDisplayConfig() {
		return displayConfig;
	}

	private final VerticalSeparatorsData separators = new VerticalSeparatorsData();

	public VerticalSeparatorsData getSeparators() {
		return separators;
	}

	private final TaskDrawRegistryData drawRegistry = new TaskDrawRegistryData();

	public TaskDrawRegistryData getDrawRegistry() {
		return drawRegistry;
	}

	private final TimelineStyleData timelineStyle;

	public TimelineStyleData getTimelineStyle() {
		return timelineStyle;
	}

	// ========================================================================
	// Layout / origin (internal, not in value objects)
	// ========================================================================
	private final RealOrigin origin = RealUtils.createOrigin();
	private double totalHeightWithoutFooter;

	// ========================================================================
	// Infrastructure
	// ========================================================================
	private final GanttStyle ganttStyle;
	private final ISkinParam skinParam;

	// ========================================================================
	// Constructor
	// ========================================================================

	public GanttPreparedModel(GanttStyle ganttStyle, ISkinParam skinParam) {
		this.ganttStyle = ganttStyle;
		this.skinParam = skinParam;
		this.timelineStyle = new TimelineStyleData(ganttStyle, HColorSet.instance());
	}

	@Override
	public TaskDraw getTaskDraw(Task task) {
		return getDrawRegistry().getTaskDraw(task);
	}

	@Override
	public PiecewiseConstant getDefaultPlan() {
		return dayCalendar.getOpenClose().asPiecewiseConstant();
	}

	@Override
	public HColorSet getIHtmlColorSet() {
		return timelineStyle.getColorSet();
	}

	// ========================================================================
	// Internal accessors
	// ========================================================================

	protected double getTotalHeightWithoutFooter() {
		return totalHeightWithoutFooter;
	}

	public ISkinParam getSkinParam() {
		return skinParam;
	}

	public Pragma getPragma() {
		return skinParam.getPragma();
	}

	// ========================================================================
	// TimeScale builders
	// ========================================================================

	public TimeScale simple() {
		return new TimeScaleWink(getTimelineStyle().getCellWidth(), getScaleConfig().getEffectiveScale(),
				getScaleConfig().getPrintScale());
	}

	public TimeScale daily() {
		return getScaleConfig().isHideClosed()
				? new TimeScaleDailyHideClosed(getTimelineStyle().getCellWidth(),
						TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
						getDayCalendar().getOpenClose())
				: new TimeScaleDaily(getTimelineStyle().getCellWidth(),
						TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
						getTimeBounds().getPrintStart());
	}

	public TimeScale weekly() {
		return new TimeScaleCompressed(getTimelineStyle().getCellWidth(),
				TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
				getTimeBounds().getPrintStart());
	}

	public TimeScale monthly() {
		return new TimeScaleCompressed(getTimelineStyle().getCellWidth(),
				TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
				getTimeBounds().getPrintStart());
	}

	public TimeScale quaterly() {
		return new TimeScaleCompressed(getTimelineStyle().getCellWidth(),
				TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
				getTimeBounds().getPrintStart());
	}

	public TimeScale yearly() {
		return new TimeScaleCompressed(getTimelineStyle().getCellWidth(),
				TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), getScaleConfig().getEffectiveScale(),
				getTimeBounds().getPrintStart());
	}

	// ========================================================================
	// TimeHeader builder
	// ========================================================================

	public TimeHeader buildTimeHeader() {
		final PrintScale scale = getScaleConfig().getPrintScale();
		if (scale == PrintScale.DAILY)
			return new TimeHeaderDaily(this);
		else if (scale == PrintScale.WEEKLY)
			return new TimeHeaderWeekly(this);
		else if (scale == PrintScale.MONTHLY)
			return new TimeHeaderMonthly(this);
		else if (scale == PrintScale.QUARTERLY)
			return new TimeHeaderQuarterly(this);
		else if (scale == PrintScale.YEARLY)
			return new TimeHeaderYearly(this);
		else
			throw new IllegalStateException();
	}

	// ========================================================================
	// Drawing helpers
	// ========================================================================

	protected TimePoint getStartForDrawing(final Task tmp) {
		TimePoint result;
		if (getTimeBounds().getPrintStart() == null)
			result = tmp.getStart();
		else
			result = TimePoint.max(TimePoint.ofStartOfDay(getTimeBounds().getMinDay()), tmp.getStart());

		return result;
	}

	protected TimePoint getEndForDrawing(final Task tmp) {
		TimePoint result;
		if (getTimeBounds().getPrintStart() == null)
			result = tmp.getEnd();
		else
			result = TimePoint.min(TimePoint.ofStartOfDay(getTimeBounds().getMaxDay().plusDays(1)), tmp.getEnd());

		return result;
	}

	public void magicPush(StringBounder stringBounder) {
		final List<TaskDraw> notes = new ArrayList<>();
		for (TaskDraw td : drawRegistry.getDrawsMap().values()) {
			final FingerPrint taskPrint = td.getFingerPrint(stringBounder);
			final FingerPrint fingerPrintNote = td.getFingerPrintNote(stringBounder);

			if (td.getTrueRow() == null)
				for (TaskDraw note : notes) {
					final FingerPrint otherNote = note.getFingerPrintNote(stringBounder);
					final double deltaY = otherNote.overlap(taskPrint);
					if (deltaY > 0) {
						final Real bottom = note.getY(stringBounder).addAtLeast(note.getHeightMax(stringBounder));
						td.getY(stringBounder).ensureBiggerThan(bottom);
						origin.compileNow();
					}

				}

			if (fingerPrintNote != null)
				notes.add(td);

		}
	}

	public double lastY(StringBounder stringBounder) {
		double result = 0;
		for (TaskDraw td : drawRegistry.getDrawsMap().values())
			result = Math.max(result, td.getY(stringBounder).getCurrentValue() + td.getHeightMax(stringBounder));

		return result;
	}

	protected TaskDraw createTaskDrawRegular(TimeScale timeScale, Real y, final Task task, final String display) {
		final boolean oddEnd;
		final boolean oddStart;
		final TimePoint startForDrawing = getStartForDrawing(task);
		final TimePoint endForDrawing = getEndForDrawing(task);
		if (getTimeBounds().getPrintStart() != null) {
			oddStart = TimePoint.ofStartOfDay(getTimeBounds().getMinDay()).compareTo(startForDrawing) == 0;
			oddEnd = TimePoint.ofStartOfDay(getTimeBounds().getMaxDay().plusDays(1)).compareTo(endForDrawing) == 0;
		} else {
			oddStart = false;
			oddEnd = false;
		}
		return new TaskDrawRegular(timeScale, y, display, startForDrawing, endForDrawing, oddStart, oddEnd,
				getSkinParam(), task, this, getConstraintsForTask(task), task.getStyleBuilder());
	}

	private Collection<GanttConstraint> getConstraintsForTask(Task task) {
		final List<GanttConstraint> result = new ArrayList<>();
		for (GanttConstraint constraint : getModelData().getConstraints())
			if (constraint.isOn(task))
				result.add(constraint);

		return Collections.unmodifiableCollection(result);
	}

	public int getLoadForResource(Resource res, TimePoint i) {
		int result = 0;
		for (Task task : getModelData().getTasks()) {
			if (task instanceof TaskSeparator)
				continue;

			final TaskImpl task2 = (TaskImpl) task;
			result += task2.loadForResource(res, i);
		}
		return result;
	}

	protected ResourceDraw buildResourceDraw(Resource res, TimeScale timeScale, double y) {
		return new ResourceDrawNumbers(this, res, timeScale, y, TimePoint.ofStartOfDay(getTimeBounds().getMinDay()),
				TimePoint.ofEndOfDayMinusOneSecond(getTimeBounds().getMaxDay()));
	}

	public void initTaskAndResourceDraws(StringBounder stringBounder, TimeHeader timeHeader) {
		final TimeScale timeScale = timeHeader.getTimeScale();
		final double fullHeaderHeight = timeHeader.getFullHeaderHeight(stringBounder);
		Real y = origin.addFixed(fullHeaderHeight);
		for (Task task : getModelData().getTasks()) {
			final TaskDraw draw;
			if (task instanceof TaskSeparator) {
				final TaskSeparator taskSeparator = (TaskSeparator) task;
				draw = new TaskDrawSeparator(taskSeparator.getName(), timeScale, y, getTimeBounds().getMinDay(),
						getTimeBounds().getMaxDay(), task.getStyleBuilder(), getSkinParam());
			} else if (task instanceof TaskGroup) {
				final TaskGroup taskGroup = (TaskGroup) task;
				draw = new TaskDrawGroup(timeScale, y, taskGroup.getCode().getDisplay(), getStartForDrawing(taskGroup),
						getEndForDrawing(taskGroup), task, this, task.getStyleBuilder(), getSkinParam());
			} else {
				final TaskImpl taskImpl = (TaskImpl) task;
				final String display = getDisplayConfig().isHideResourceName() ? taskImpl.getCode().getDisplay()
						: taskImpl.getPrettyDisplay();
				if (taskImpl.isDiamond())
					draw = new TaskDrawDiamond(timeScale, y, display, getStartForDrawing(taskImpl), taskImpl, this,
							task.getStyleBuilder(), getSkinParam());
				else
					draw = createTaskDrawRegular(timeScale, y, taskImpl, display);

				draw.setColorsAndCompletion(taskImpl.getColors(), taskImpl.getCompletion(), taskImpl.getUrl(),
						taskImpl.getNote(), taskImpl.getNoteStereotype());
			}
			if (task.getRow() == null)
				y = y.addAtLeast(draw.getFullHeightTask(stringBounder));

			drawRegistry.putTaskDraw(task, draw);
		}
		origin.compileNow();
		magicPush(stringBounder);
		double yy = lastY(stringBounder);
		if (yy == 0) {
			yy = fullHeaderHeight;
		} else if (getDisplayConfig().isHideResourceFootbox() == false)
			for (Resource res : getModelData().getResources()) {
				final ResourceDraw draw = buildResourceDraw(res, timeScale, yy);
				res.setTaskDraw(draw);
				yy += draw.getHeight(stringBounder);
			}

		totalHeightWithoutFooter = yy;
	}

	public boolean isHidden(Task task) {
		if (getTimeBounds().getPrintStart() == null || task instanceof TaskSeparator)
			return false;

		if (task.getEndMinusOneDayTOBEREMOVED().compareTo(TimePoint.ofStartOfDay(getTimeBounds().getMinDay())) < 0)
			return true;

		if (task.getStart().compareTo(TimePoint.ofEndOfDayMinusOneSecond(getTimeBounds().getMaxDay())) > 0)
			return true;

		return false;
	}

}
