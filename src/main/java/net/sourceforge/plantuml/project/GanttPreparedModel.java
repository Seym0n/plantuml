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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.color.HColorSet;
import net.sourceforge.plantuml.klimt.drawing.UGraphic;
import net.sourceforge.plantuml.klimt.font.StringBounder;
import net.sourceforge.plantuml.klimt.font.UFont;
import net.sourceforge.plantuml.klimt.geom.HorizontalAlignment;
import net.sourceforge.plantuml.project.core.PrintScale;
import net.sourceforge.plantuml.project.core.Resource;
import net.sourceforge.plantuml.project.core.Task;
import net.sourceforge.plantuml.project.core.TaskCode;
import net.sourceforge.plantuml.project.core.TaskGroup;
import net.sourceforge.plantuml.project.core.TaskImpl;
import net.sourceforge.plantuml.project.core.TaskSeparator;
import net.sourceforge.plantuml.project.draw.FingerPrint;
import net.sourceforge.plantuml.project.draw.ResourceDraw;
import net.sourceforge.plantuml.project.draw.ResourceDrawNumbers;
import net.sourceforge.plantuml.project.draw.TaskDraw;
import net.sourceforge.plantuml.project.draw.TaskDrawDiamond;
import net.sourceforge.plantuml.project.draw.TaskDrawGroup;
import net.sourceforge.plantuml.project.draw.TaskDrawRegular;
import net.sourceforge.plantuml.project.draw.TaskDrawSeparator;
import net.sourceforge.plantuml.project.draw.WeeklyHeaderStrategy;
import net.sourceforge.plantuml.project.draw.header.TimeHeader;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderDaily;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderMonthly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderQuarterly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderWeekly;
import net.sourceforge.plantuml.project.draw.header.TimeHeaderYearly;
import net.sourceforge.plantuml.project.ngm.math.PiecewiseConstant;
import net.sourceforge.plantuml.project.time.TimePoint;
import net.sourceforge.plantuml.project.time.WeekNumberStrategy;
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
import net.sourceforge.plantuml.style.PName;
import net.sourceforge.plantuml.style.SName;
import net.sourceforge.plantuml.style.Style;

public class GanttPreparedModel implements ToTaskDraw, GanttModel, TimeBounds, TimeScaleConfig,
		WeekConfig, DayCalendar, DisplayConfig, TimelineStyle, VerticalSeparators, TaskDrawRegistry, LocaleProvider {

	// ------------------------------------------------------------------------
	// core domain data
	// ------------------------------------------------------------------------
	private final List<GanttConstraint> constraints = new ArrayList<>();
	private final Map<TaskCode, Task> tasks = new LinkedHashMap<>();
	private final Map<String, Resource> resources = new LinkedHashMap<>();

	// ------------------------------------------------------------------------
	// layout / origin
	// ------------------------------------------------------------------------
	private final RealOrigin origin = RealUtils.createOrigin();

	// ------------------------------------------------------------------------
	// inputs / configuration
	// ------------------------------------------------------------------------
	private final GanttStyle ganttStyle;

	private Locale locale = Locale.ENGLISH;

	private PrintScale printScale = PrintScale.DAILY;
	private double factorScale = 1.0;

	private boolean hideClosed;
	private LocalDate printStart;
	private LocalDate printEnd;

	private WeeklyHeaderStrategy weeklyHeaderStrategy;
	private int weekStartingNumber;

	// Let's follow ISO-8601 rules
	private WeekNumberStrategy weekNumberStrategy = new WeekNumberStrategy(DayOfWeek.MONDAY, 4);

	private LabelStrategy labelStrategy = new LabelStrategy(LabelPosition.LEGACY, HorizontalAlignment.LEFT);

	private boolean showFootbox = true;
	private boolean hideResourceName;
	private boolean hideResourceFootbox;

	// ------------------------------------------------------------------------
	// model bounds / computed scalars
	// ------------------------------------------------------------------------
	private LocalDate minDay = TimePoint.epoch();
	private LocalDate maxDay;

	private double totalHeightWithoutFooter;

	// ------------------------------------------------------------------------
	// prepared drawing / layout artifacts
	// ------------------------------------------------------------------------
	private final Map<Task, TaskDraw> draws = new LinkedHashMap<>();
	private final Set<TimePoint> verticalSeparatorBefore = new HashSet<>();

	// ------------------------------------------------------------------------
	// timeline labels and colors (prepared caches)
	// ------------------------------------------------------------------------
	private final Map<TimePoint, String> nameDays = new HashMap<>();

	private final Map<TimePoint, HColor> colorDaysToday = new HashMap<>();
	private final Map<TimePoint, HColor> colorDaysInternal = new HashMap<>();
	private final Map<DayOfWeek, HColor> colorDaysOfWeek = new HashMap<>();

	// ------------------------------------------------------------------------
	// internal helpers / shared infrastructure
	// ------------------------------------------------------------------------
	private final OpenClose openClose = new OpenClose();
	private final HColorSet colorSet = HColorSet.instance();
	private final ISkinParam skinParam;

	// ========================================================================
	// Constructor
	// ========================================================================

	public GanttPreparedModel(GanttStyle ganttStyle, ISkinParam skinParam) {
		this.ganttStyle = ganttStyle;
		this.skinParam = skinParam;
	}

	// ========================================================================
	// GanttModel implementation
	// ========================================================================

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

	// ========================================================================
	// TimeBounds implementation
	// ========================================================================

	@Override
	public LocalDate getMinDay() {
		return minDay;
	}

	@Override
	public LocalDate getMaxDay() {
		return maxDay;
	}

	@Override
	public LocalDate getPrintStart() {
		return printStart;
	}

	@Override
	public LocalDate getPrintEnd() {
		return printEnd;
	}

	// ========================================================================
	// TimeScaleConfig implementation
	// ========================================================================

	@Override
	public PrintScale getPrintScale() {
		return printScale;
	}

	@Override
	public double getFactorScale() {
		return factorScale;
	}

	@Override
	public double getEffectiveScale() {
		return printScale.getDefaultScale() * factorScale;
	}

	@Override
	public boolean isHideClosed() {
		return hideClosed;
	}

	// ========================================================================
	// WeekConfig implementation
	// ========================================================================

	@Override
	public WeekNumberStrategy getWeekNumberStrategy() {
		return weekNumberStrategy;
	}

	@Override
	public WeeklyHeaderStrategy getWeeklyHeaderStrategy() {
		return weeklyHeaderStrategy;
	}

	@Override
	public int getWeekStartingNumber() {
		return weekStartingNumber;
	}

	// ========================================================================
	// DayCalendar implementation
	// ========================================================================

	@Override
	public boolean isOpen(LocalDate day) {
		return openClose.getLoadAtDUMMY(day) > 0;
	}

	@Override
	public boolean isOpen(TimePoint instant) {
		return openClose.getLoadAtDUMMY(instant.toDay()) > 0;
	}

	@Override
	public HColor getDayColor(TimePoint day) {
		HColor color = colorDaysToday.get(day);
		if (color == null)
			color = colorDaysInternal.get(day);
		return color;
	}

	@Override
	public HColor getDayOfWeekColor(DayOfWeek dayOfWeek) {
		return colorDaysOfWeek.get(dayOfWeek);
	}

	@Override
	public String getDayName(TimePoint day) {
		return nameDays.get(day);
	}

	@Override
	public OpenClose getOpenClose() {
		return openClose;
	}

	// ========================================================================
	// DisplayConfig implementation
	// ========================================================================

	@Override
	public LabelStrategy getLabelStrategy() {
		return labelStrategy;
	}

	@Override
	public boolean isShowFootbox() {
		return showFootbox;
	}

	@Override
	public boolean isHideResourceName() {
		return hideResourceName;
	}

	@Override
	public boolean isHideResourceFootbox() {
		return hideResourceFootbox;
	}

	// ========================================================================
	// TimelineStyle implementation
	// ========================================================================

	@Override
	public double getFontSizeDay() {
		return getStyleDay().value(PName.FontSize).asDouble();
	}

	@Override
	public double getFontSizeMonth() {
		return ganttStyle.getStyle(SName.timeline, SName.month).value(PName.FontSize).asDouble();
	}

	@Override
	public double getFontSizeYear() {
		return ganttStyle.getStyle(SName.timeline, SName.year).value(PName.FontSize).asDouble();
	}

	@Override
	public UFont getFont(SName param) {
		return ganttStyle.getStyle(SName.timeline, param).getUFont();
	}

	@Override
	public HColor getClosedBackgroundColor() {
		return ganttStyle.getStyle(SName.closed).value(PName.BackGroundColor).asColor(colorSet);
	}

	@Override
	public HColor getClosedFontColor() {
		return ganttStyle.getStyle(SName.closed).value(PName.FontColor).asColor(colorSet);
	}

	@Override
	public HColor getOpenFontColor() {
		return ganttStyle.getStyle(SName.timeline).value(PName.FontColor).asColor(colorSet);
	}

	@Override
	public HColor getLineColor() {
		return ganttStyle.getStyle(SName.timeline).value(PName.LineColor).asColor(colorSet);
	}

	@Override
	public HColorSet getColorSet() {
		return colorSet;
	}

	@Override
	public UGraphic applyVerticalSeparatorStyle(UGraphic ug) {
		final Style style = ganttStyle.getStyle(SName.verticalSeparator);
		final HColor color = style.value(PName.LineColor).asColor(colorSet);
		final UStroke stroke = style.getStroke();
		return ug.apply(color).apply(stroke);
	}

	@Override
	public double getCellWidth() {
		final double w = getStyleDay().value(PName.FontSize).asDouble();
		return w * 1.6;
	}

	// ========================================================================
	// VerticalSeparators implementation
	// ========================================================================

	@Override
	public boolean hasSeparatorBefore(TimePoint day) {
		return verticalSeparatorBefore.contains(day);
	}

	// ========================================================================
	// TaskDrawRegistry implementation
	// ========================================================================

	@Override
	public TaskDraw getTaskDraw(Task task) {
		return draws.get(task);
	}

	// ========================================================================
	// LocaleProvider implementation
	// ========================================================================

	@Override
	public Locale getLocale() {
		return locale;
	}

	// ========================================================================
	// ToTaskDraw implementation (existing interface)
	// ========================================================================

	@Override
	public PiecewiseConstant getDefaultPlan() {
		return openClose.asPiecewiseConstant();
	}

	@Override
	public HColorSet getIHtmlColorSet() {
		return colorSet;
	}

	// ========================================================================
	// Setters for configuration (used during parsing)
	// ========================================================================

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setPrintScale(PrintScale printScale) {
		this.printScale = printScale;
	}

	public void setFactorScale(double factorScale) {
		this.factorScale = factorScale;
	}

	public void setHideClosed(boolean hideClosed) {
		this.hideClosed = hideClosed;
	}

	public void setPrintStart(LocalDate printStart) {
		this.printStart = printStart;
	}

	public void setPrintEnd(LocalDate printEnd) {
		this.printEnd = printEnd;
	}

	public void setWeeklyHeaderStrategy(WeeklyHeaderStrategy weeklyHeaderStrategy) {
		this.weeklyHeaderStrategy = weeklyHeaderStrategy;
	}

	public void setWeekStartingNumber(int weekStartingNumber) {
		this.weekStartingNumber = weekStartingNumber;
	}

	public void setWeekNumberStrategy(WeekNumberStrategy weekNumberStrategy) {
		this.weekNumberStrategy = weekNumberStrategy;
	}

	public void setLabelStrategy(LabelStrategy labelStrategy) {
		this.labelStrategy = labelStrategy;
	}

	public void setShowFootbox(boolean showFootbox) {
		this.showFootbox = showFootbox;
	}

	public void setHideResourceName(boolean hideResourceName) {
		this.hideResourceName = hideResourceName;
	}

	public void setHideResourceFootbox(boolean hideResourceFootbox) {
		this.hideResourceFootbox = hideResourceFootbox;
	}

	public void setMinDay(LocalDate minDay) {
		this.minDay = minDay;
	}

	public void setMaxDay(LocalDate maxDay) {
		this.maxDay = maxDay;
	}

	// ========================================================================
	// Mutators for collections (used during parsing)
	// ========================================================================

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

	public void addVerticalSeparatorBefore(TimePoint day) {
		verticalSeparatorBefore.add(day);
	}

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

	// ========================================================================
	// Internal accessors (for subclasses and package)
	// ========================================================================

	protected Map<TaskCode, Task> getTasksMap() {
		return tasks;
	}

	protected Map<String, Resource> getResourcesMap() {
		return resources;
	}

	protected Map<Task, TaskDraw> getDrawsMap() {
		return draws;
	}

	protected RealOrigin getOrigin() {
		return origin;
	}

	protected double getTotalHeightWithoutFooter() {
		return totalHeightWithoutFooter;
	}

	protected void setTotalHeightWithoutFooter(double totalHeightWithoutFooter) {
		this.totalHeightWithoutFooter = totalHeightWithoutFooter;
	}

	public Map<TimePoint, String> getNameDays() {
		return Collections.unmodifiableMap(nameDays);
	}

	/**
	 * Returns a merged view of all day colors (today colors override internal colors).
	 */
	public Map<TimePoint, HColor> getColorDays() {
		final Map<TimePoint, HColor> result = new HashMap<>(colorDaysInternal);
		result.putAll(colorDaysToday);
		return Collections.unmodifiableMap(result);
	}

	// ========================================================================
	// Style helpers
	// ========================================================================

	private Style getStyleDay() {
		return ganttStyle.getStyle(SName.timeline, SName.day);
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
		return new TimeScaleWink(getCellWidth(), getEffectiveScale(), printScale);
	}

	public TimeScale daily() {
		return hideClosed
				? new TimeScaleDailyHideClosed(getCellWidth(), TimePoint.ofStartOfDay(minDay),
						getEffectiveScale(), openClose)
				: new TimeScaleDaily(getCellWidth(), TimePoint.ofStartOfDay(minDay), getEffectiveScale(),
						printStart);
	}

	public TimeScale weekly() {
		return new TimeScaleCompressed(getCellWidth(), TimePoint.ofStartOfDay(minDay), getEffectiveScale(),
				printStart);
	}

	public TimeScale monthly() {
		return new TimeScaleCompressed(getCellWidth(), TimePoint.ofStartOfDay(minDay), getEffectiveScale(),
				printStart);
	}

	public TimeScale quaterly() {
		return new TimeScaleCompressed(getCellWidth(), TimePoint.ofStartOfDay(minDay), getEffectiveScale(),
				printStart);
	}

	public TimeScale yearly() {
		return new TimeScaleCompressed(getCellWidth(), TimePoint.ofStartOfDay(minDay), getEffectiveScale(),
				printStart);
	}

	// ========================================================================
	// TimeHeader builder
	// ========================================================================

	public TimeHeader buildTimeHeader() {
		if (printScale == PrintScale.DAILY)
			return new TimeHeaderDaily(this);
		else if (printScale == PrintScale.WEEKLY)
			return new TimeHeaderWeekly(this);
		else if (printScale == PrintScale.MONTHLY)
			return new TimeHeaderMonthly(this);
		else if (printScale == PrintScale.QUARTERLY)
			return new TimeHeaderQuarterly(this);
		else if (printScale == PrintScale.YEARLY)
			return new TimeHeaderYearly(this);
		else
			throw new IllegalStateException();
	}

	// ========================================================================
	// Drawing helpers
	// ========================================================================

	protected TimePoint getStartForDrawing(final Task tmp) {
		TimePoint result;
		if (printStart == null)
			result = tmp.getStart();
		else
			result = TimePoint.max(TimePoint.ofStartOfDay(minDay), tmp.getStart());

		return result;
	}

	protected TimePoint getEndForDrawing(final Task tmp) {
		TimePoint result;
		if (printStart == null)
			result = tmp.getEnd();
		else
			result = TimePoint.min(TimePoint.ofStartOfDay(maxDay.plusDays(1)), tmp.getEnd());

		return result;
	}

	public void magicPush(StringBounder stringBounder) {
		final List<TaskDraw> notes = new ArrayList<>();
		for (TaskDraw td : draws.values()) {
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
		for (TaskDraw td : draws.values())
			result = Math.max(result, td.getY(stringBounder).getCurrentValue() + td.getHeightMax(stringBounder));

		return result;
	}

	protected TaskDraw createTaskDrawRegular(TimeScale timeScale, Real y, final Task task, final String display) {
		final boolean oddEnd;
		final boolean oddStart;
		final TimePoint startForDrawing = getStartForDrawing(task);
		final TimePoint endForDrawing = getEndForDrawing(task);
		if (printStart != null) {
			oddStart = TimePoint.ofStartOfDay(minDay).compareTo(startForDrawing) == 0;
			oddEnd = TimePoint.ofStartOfDay(maxDay.plusDays(1)).compareTo(endForDrawing) == 0;
		} else {
			oddStart = false;
			oddEnd = false;
		}
		return new TaskDrawRegular(timeScale, y, display, startForDrawing, endForDrawing, oddStart, oddEnd,
				getSkinParam(), task, this, getConstraintsForTask(task), task.getStyleBuilder());
	}

	private Collection<GanttConstraint> getConstraintsForTask(Task task) {
		final List<GanttConstraint> result = new ArrayList<>();
		for (GanttConstraint constraint : constraints)
			if (constraint.isOn(task))
				result.add(constraint);

		return Collections.unmodifiableCollection(result);
	}

	public int getLoadForResource(Resource res, TimePoint i) {
		int result = 0;
		for (Task task : tasks.values()) {
			if (task instanceof TaskSeparator)
				continue;

			final TaskImpl task2 = (TaskImpl) task;
			result += task2.loadForResource(res, i);
		}
		return result;
	}

	protected ResourceDraw buildResourceDraw(Resource res, TimeScale timeScale, double y) {
		return new ResourceDrawNumbers(this, res, timeScale, y, TimePoint.ofStartOfDay(minDay),
				TimePoint.ofEndOfDayMinusOneSecond(maxDay));
	}

	public void initTaskAndResourceDraws(StringBounder stringBounder, TimeHeader timeHeader) {
		final TimeScale timeScale = timeHeader.getTimeScale();
		final double fullHeaderHeight = timeHeader.getFullHeaderHeight(stringBounder);
		Real y = origin.addFixed(fullHeaderHeight);
		for (Task task : tasks.values()) {
			final TaskDraw draw;
			if (task instanceof TaskSeparator) {
				final TaskSeparator taskSeparator = (TaskSeparator) task;
				draw = new TaskDrawSeparator(taskSeparator.getName(), timeScale, y, minDay, maxDay,
						task.getStyleBuilder(), getSkinParam());
			} else if (task instanceof TaskGroup) {
				final TaskGroup taskGroup = (TaskGroup) task;
				draw = new TaskDrawGroup(timeScale, y, taskGroup.getCode().getDisplay(), getStartForDrawing(taskGroup),
						getEndForDrawing(taskGroup), task, this, task.getStyleBuilder(), getSkinParam());
			} else {
				final TaskImpl taskImpl = (TaskImpl) task;
				final String display = hideResourceName ? taskImpl.getCode().getDisplay() : taskImpl.getPrettyDisplay();
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

			draws.put(task, draw);
		}
		origin.compileNow();
		magicPush(stringBounder);
		double yy = lastY(stringBounder);
		if (yy == 0) {
			yy = fullHeaderHeight;
		} else if (hideResourceFootbox == false)
			for (Resource res : resources.values()) {
				final ResourceDraw draw = buildResourceDraw(res, timeScale, yy);
				res.setTaskDraw(draw);
				yy += draw.getHeight(stringBounder);
			}

		totalHeightWithoutFooter = yy;
	}

	protected boolean isHidden(Task task) {
		if (printStart == null || task instanceof TaskSeparator)
			return false;

		if (task.getEndMinusOneDayTOBEREMOVED().compareTo(TimePoint.ofStartOfDay(minDay)) < 0)
			return true;

		if (task.getStart().compareTo(TimePoint.ofEndOfDayMinusOneSecond(maxDay)) > 0)
			return true;

		return false;
	}

}
