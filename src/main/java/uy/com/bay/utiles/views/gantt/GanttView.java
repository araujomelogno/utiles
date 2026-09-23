package uy.com.bay.utiles.views.gantt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.element.StepElement;
import org.vaadin.tltv.gantt.event.StepClickEvent;
import org.vaadin.tltv.gantt.model.Resolution;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.dom.Style.Display;
import com.vaadin.flow.dom.Style.Position;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.FieldworkType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.GanttService;

@Route(value = "gantt")
@PermitAll
public class GanttView extends VerticalLayout {
	private static final String ALL_TYPES = "TODOS";
	// Dias con completos dentro / fuera de la fecha planificada del fieldwork.
	private static final String IN_PLAN_COLOR = "#ADD8E6";
	private static final String OUT_OF_PLAN_COLOR = "#F4A6A6";

	private final GanttService ganttService;
	private Gantt gantt;
	private FlexLayout scrollWrapper;
	private TreeGrid<Step> treeGrid;

	private DatePicker startDateField;
	private DatePicker endDateField;
	private ComboBox<String> typeFilterComboBox;
	private Button filterbutton;
	private Button zoomButton;
	// false: totales de casos por mes; true: zoom in a casos por dia.
	private boolean dailyZoom = false;
	private Double budgetTotal = 0d;
	private Double totalSpent = 0d;

	private int clickedBackgroundIndex;
	private int totalgoal = 0;
	private int totalCompleted = 0;
	private final Map<String, Fieldwork> stepToFieldworkMap;
	private final Map<String, Study> stepToStudyMap;
	// Filas de fieldwork que ya tienen dibujados sus sub-steps por dia.
	private final Set<String> fieldworkStepsWithDailySubSteps = new HashSet<>();

	public GanttView(GanttService ganttService) {
		this.ganttService = ganttService;
		this.stepToFieldworkMap = new HashMap<>();
		this.stepToStudyMap = new HashMap<>();
		setWidthFull();
		setPadding(false);

		gantt = createGantt();
		gantt.setWidth("70%");

		gantt.setMovableStepsBetweenRows(false);

		Div controlPanel = buildControlPanel();
		buildCaptionTreeGrid();

		scrollWrapper = new FlexLayout();
		scrollWrapper.setId("scroll-wrapper");
		scrollWrapper.setMinHeight("0");
		scrollWrapper.setWidthFull();
		scrollWrapper.add(gantt);
		scrollWrapper.addComponentAsFirst(treeGrid);

		add(controlPanel, scrollWrapper);
	}

	private void buildCaptionTreeGrid() {

		treeGrid = gantt.buildCaptionTreeGrid("Proyecto");
		treeGrid.setWidth("30%");
		treeGrid.setAllRowsVisible(true);
		treeGrid.getStyle().set("--gantt-caption-grid-row-height", "30px");
		// Las filas de fieldwork se agregan al gantt al expandir su estudio (y se
		// quitan al colapsarlo), por eso los completos por dia se dibujan al expandir.
		treeGrid.addExpandListener(event -> event.getItems().forEach(
				parent -> treeGrid.getTreeData().getChildren(parent).forEach(this::addDailySubSteps)));
		treeGrid.addCollapseListener(event -> event.getItems().forEach(parent -> treeGrid.getTreeData()
				.getChildren(parent).forEach(child -> fieldworkStepsWithDailySubSteps.remove(child.getUid()))));

		gantt.setMovableStepsBetweenRows(false);
		gantt.setMovableSteps(false);
		gantt.setResizableSteps(false);

		fillGantt();
	}

	private void fillGantt() {
		List<Fieldwork> fieldworks = ganttService.getFieldworks(gantt.getStartDate(), gantt.getEndDate());
		String selectedType = typeFilterComboBox.getValue();
		if (selectedType != null && !ALL_TYPES.equals(selectedType)) {
			FieldworkType filterType = FieldworkType.valueOf(selectedType);
			fieldworks = fieldworks.stream().filter(fw -> fw.getType() == filterType).collect(Collectors.toList());
		}
		Map<Study, List<Fieldwork>> fieldworksByStudy = fieldworks.stream()
				.collect(Collectors.groupingBy(Fieldwork::getStudy));
		fieldworksByStudy.forEach((study, fieldworkList) -> {
			Step studyStep = new Step();

			studyStep.setCaption(study.getName());
			studyStep.setUid(UUID.randomUUID().toString());
			fieldworkList.stream().map(Fieldwork::getInitPlannedDate).filter(Objects::nonNull).min(LocalDate::compareTo)
					.ifPresent(minDate -> studyStep.setStartDate(minDate.atStartOfDay()));
			fieldworkList.stream().map(Fieldwork::getEndPlannedDate).filter(Objects::nonNull).max(LocalDate::compareTo)
					.ifPresent(maxDate -> studyStep.setEndDate(maxDate.atStartOfDay()));
			studyStep.setBackgroundColor("#eb590580");
			studyStep.setMovable(false);

			stepToStudyMap.put(studyStep.getUid(), study);
			gantt.addStep(0, studyStep);
			fieldworkList.forEach(fieldwork -> {
				if (fieldwork.getInitPlannedDate() != null && fieldwork.getEndPlannedDate() != null) {
					Step subStep = new Step();
					subStep.setCaption(fieldwork.getType().toString() + " - Objetivo:" + fieldwork.getGoalQuantity()
							+ " Completas:" + fieldwork.getCompleted());
					subStep.setStartDate(fieldwork.getInitPlannedDate().atStartOfDay());
					subStep.setEndDate(fieldwork.getEndPlannedDate().atStartOfDay());
					if (dailyZoom) {
						// Se estira la fila para que entren los dias con completos fuera de la
						// fecha planificada (se pintan en rojo en addDailySubSteps).
						TreeMap<LocalDate, Integer> visibleDays = visibleCompletedByDay(fieldwork);
						if (!visibleDays.isEmpty()) {
							LocalDate firstDay = visibleDays.firstKey();
							LocalDate lastDay = visibleDays.lastKey();
							if (firstDay.isBefore(fieldwork.getInitPlannedDate()))
								subStep.setStartDate(firstDay.atStartOfDay());
							if (!lastDay.isBefore(fieldwork.getEndPlannedDate()))
								subStep.setEndDate(lastDay.plusDays(1).atStartOfDay());
						}
					}
					String uid = UUID.randomUUID().toString();
					subStep.setUid(uid);
					subStep.setBackgroundColor("#E6E6E6");
					subStep.setMovable(false);
					treeGrid.getTreeData().addItem(studyStep, subStep);
					stepToFieldworkMap.put(uid, fieldwork);
					addDailySubSteps(subStep);
					if (fieldwork.getGoalQuantity() != null)
						totalgoal = totalgoal + fieldwork.getGoalQuantity();
					if (fieldwork.getCompleted() != null)
						totalCompleted = totalCompleted + fieldwork.getCompleted();

				}
			});

			totalSpent = 0d;
			budgetTotal = 0d;
			if (study.getBudget() != null)
				study.getBudget().getEntries().forEach(entry -> {
					totalSpent = totalSpent + entry.getSpent();
					budgetTotal = budgetTotal + entry.getTotal();
				});
			if (budgetTotal != 0d) {
				if (totalSpent > budgetTotal)
					gantt.getStepElement(studyStep.getUid()).add(createBudgetBar(100d));
				else
					gantt.getStepElement(studyStep.getUid()).add(createBudgetBar(100 * totalSpent / budgetTotal));
			}
			if (totalgoal != 0) {
				gantt.getStepElement(studyStep.getUid()).add(createProgressBar(100 * totalCompleted / totalgoal));

			}

			totalgoal = 0;
			totalCompleted = 0;
		});

		LocalDate start = startDateField.getValue();
		LocalDate end = endDateField.getValue();
		if (start != null && end != null) {
			// En la vista por mes se exige al menos un mes de rango (como antes); en la
			// vista por dia alcanza con que el rango no sea vacio.
			boolean showTotals = dailyZoom ? start.isBefore(end) : start.plusMonths(1).isBefore(end);
			if (showTotals) {
				addTotalsStep("Total Casos Calle", FieldworkType.CALLE, fieldworks, start, end);
				addTotalsStep("Total Casos Telefónico", FieldworkType.TELEFONICO, fieldworks, start, end);
				addTotalsStep("Total Casos Web", FieldworkType.WEB, fieldworks, start, end);
			}
		}
	}

	/**
	 * Agrega una fila de totales para el tipo de fieldwork indicado. En la vista
	 * por mes muestra, para cada mes, el objetivo de los fieldworks activos y los
	 * completos del mes (completedByMonth). En la vista por dia muestra los
	 * completos de cada dia (completedByDay).
	 */
	private void addTotalsStep(String caption, FieldworkType type, List<Fieldwork> fieldworks, LocalDate start,
			LocalDate end) {
		Step totalStep = new Step();
		totalStep.setCaption(caption);
		totalStep.setUid(UUID.randomUUID().toString());
		totalStep.setStartDate(start.atStartOfDay());
		totalStep.setEndDate(end.atStartOfDay());
		totalStep.setMovable(false);
		gantt.addStep(totalStep);

		List<Fieldwork> ofType = fieldworks.stream().filter(fw -> fw.getType() == type).collect(Collectors.toList());

		if (dailyZoom) {
			Map<LocalDate, Integer> completedByDay = new TreeMap<>();
			for (Fieldwork fw : ofType) {
				fw.getCompletedByDay().forEach((date, completed) -> {
					if (date != null && completed != null)
						completedByDay.merge(toLocalDate(date), completed, Integer::sum);
				});
			}
			completedByDay.forEach((day, completed) -> {
				if (completed == 0 || day.isBefore(start) || !day.isBefore(end))
					return;
				SubStep subStep = new SubStep(totalStep);
				subStep.setCaption(String.valueOf(completed));
				subStep.setStartDate(day.atStartOfDay());
				subStep.setEndDate(day.plusDays(1).atStartOfDay());
				subStep.setUid(UUID.randomUUID().toString());
				subStep.setBackgroundColor("#ADD8E6");
				subStep.setMovable(false);
				gantt.addSubStep(subStep);
			});
			return;
		}

		Map<YearMonth, Integer> completedByMonth = new HashMap<>();
		for (Fieldwork fw : ofType) {
			fw.getCompletedByMonth().forEach((date, completed) -> {
				if (date != null && completed != null)
					completedByMonth.merge(YearMonth.from(toLocalDate(date)), completed, Integer::sum);
			});
		}

		LocalDate currentDate = start;
		while (currentDate.isBefore(end)) {
			LocalDate startOfMonth = currentDate.withDayOfMonth(1);
			LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
			int casosDelMes = ofType.stream()
					.filter(fw -> !fw.getInitPlannedDate().isAfter(endOfMonth)
							&& !fw.getEndPlannedDate().isBefore(startOfMonth))
					.mapToInt(Fieldwork::getGoalQuantity).sum();
			int completasDelMes = completedByMonth.getOrDefault(YearMonth.from(startOfMonth), 0);

			SubStep subStep = new SubStep(totalStep);
			subStep.setCaption("Casos del mes: " + casosDelMes + " - Completas: " + completasDelMes);
			subStep.setStartDate(startOfMonth.atStartOfDay());
			subStep.setEndDate(endOfMonth.atStartOfDay().plusDays(1));
			subStep.setUid(UUID.randomUUID().toString());
			subStep.setBackgroundColor("#ADD8E6");
			subStep.setMovable(false);
			gantt.addSubStep(subStep);
			currentDate = currentDate.plusMonths(1);
		}
	}

	/**
	 * En la vista por dia agrega a la fila del fieldwork un sub-step por cada dia
	 * con completos (completedByDay), con la cantidad como caption. Solo se puede
	 * hacer cuando la fila ya esta en el gantt (estudio expandido).
	 */
	private void addDailySubSteps(Step fieldworkStep) {
		Fieldwork fieldwork = stepToFieldworkMap.get(fieldworkStep.getUid());
		if (!dailyZoom || fieldwork == null || fieldworkStepsWithDailySubSteps.contains(fieldworkStep.getUid())) {
			return;
		}
		try {
			if (gantt.getStepElement(fieldworkStep.getUid()) == null) {
				return;
			}
		} catch (RuntimeException e) {
			// La fila todavia no esta en el gantt (estudio colapsado).
			return;
		}
		LocalDate plannedStart = fieldwork.getInitPlannedDate();
		LocalDate plannedEnd = fieldwork.getEndPlannedDate();
		visibleCompletedByDay(fieldwork).forEach((day, completed) -> {
			SubStep daySubStep = new SubStep(fieldworkStep);
			daySubStep.setCaption(String.valueOf(completed));
			daySubStep.setStartDate(day.atStartOfDay());
			daySubStep.setEndDate(day.plusDays(1).atStartOfDay());
			String uid = UUID.randomUUID().toString();
			daySubStep.setUid(uid);
			boolean outOfPlan = (plannedStart != null && day.isBefore(plannedStart))
					|| (plannedEnd != null && day.isAfter(plannedEnd));
			daySubStep.setBackgroundColor(outOfPlan ? OUT_OF_PLAN_COLOR : IN_PLAN_COLOR);
			daySubStep.setMovable(false);
			gantt.addSubStep(daySubStep);
			// Al hacer click en un dia se abre el detalle del fieldwork.
			stepToFieldworkMap.put(uid, fieldwork);
		});
		fieldworkStepsWithDailySubSteps.add(fieldworkStep.getUid());
	}

	/**
	 * Completos por dia del fieldwork (solo dias con completos y dentro del rango
	 * del gantt), ordenados por fecha.
	 */
	private TreeMap<LocalDate, Integer> visibleCompletedByDay(Fieldwork fieldwork) {
		LocalDate start = gantt.getStartDate();
		LocalDate end = gantt.getEndDate();
		TreeMap<LocalDate, Integer> result = new TreeMap<>();
		fieldwork.getCompletedByDay().forEach((date, completed) -> {
			if (date != null && completed != null)
				result.merge(toLocalDate(date), completed, Integer::sum);
		});
		result.entrySet().removeIf(e -> e.getValue() == 0 || (start != null && e.getKey().isBefore(start))
				|| (end != null && !e.getKey().isBefore(end)));
		return result;
	}

	private static LocalDate toLocalDate(Date date) {
		// Las claves vienen como java.sql.Date (MapKeyTemporal DATE), que no soporta
		// toInstant(): se convierte a partir de los milisegundos.
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private Gantt createGantt() {
		Gantt gantt = new Gantt();
		gantt.setResolution(Resolution.Day);
		gantt.setStartDate(LocalDate.now().minusMonths(1));
		gantt.setEndDate(LocalDate.now().plusMonths(6));
		gantt.setLocale(UI.getCurrent().getLocale());
		gantt.setTimeZone(TimeZone.getDefault());

		gantt.addStepClickListener(this::onGanttStepClick);
		gantt.addStepMoveListener(event -> {
			Notification.show("Moved step : " + event.getAnyStep().getCaption());

			// dates and position are synchronized automatically to server side model
		});
		gantt.addStepResizeListener(event -> {
			Notification.show("Resized step : " + event.getAnyStep().getCaption());

			event.getAnyStep().setStartDate(event.getStart());
			event.getAnyStep().setEndDate(event.getEnd());

			if (event.getAnyStep().isSubstep()) {
				((SubStep) event.getAnyStep()).updateOwnerDatesBySubStep();
				event.getSource().refresh(((SubStep) event.getAnyStep()).getOwner().getUid());
			}
		});
		// Add dynamic context menu for gantt background. Clicked index is registered
		// via addGanttClickListener and addStepClickListener.
		addDynamicBackgroundContextMenu(gantt);
		return gantt;
	}

	private ProgressBar createProgressBar(double initialProgress) {
		if (initialProgress > 100)
			initialProgress = 100;
		ProgressBar bar = new ProgressBar(0, 100);
		bar.setHeight("20%");
		bar.setWidth("100%");
		bar.getStyle().setDisplay(Display.INLINE_BLOCK);
		bar.getStyle().setBottom("0");
		bar.getStyle().setPosition(Position.ABSOLUTE);
		bar.getStyle().setMargin("0");
		bar.setValue(initialProgress);
		return bar;
	}

	private ProgressBar createBudgetBar(double actualCost) {
		if (actualCost > 100)
			actualCost = 100;
		ProgressBar bar = new ProgressBar(0, 100);
		bar.setHeight("20%");
		bar.setWidth("100%");
		bar.getStyle().setDisplay(Display.INLINE_BLOCK);
		bar.getStyle().setBackground("#93ad6f");

		bar.getStyle().setTop("0");
		bar.getStyle().setPosition(Position.ABSOLUTE);
		bar.getStyle().setMargin("0");
		bar.setValue(actualCost);
		return bar;
	}

	private void addDynamicBackgroundContextMenu(Gantt gantt) {
		ContextMenu backgroundContextMenu = new ContextMenu();
		backgroundContextMenu.setTarget(gantt);
		gantt.getElement().addEventListener("vaadin-context-menu-before-open", event -> {
			backgroundContextMenu.removeAll();
			var targetStep = gantt.getStepsList().get(clickedBackgroundIndex);
			backgroundContextMenu.add(new Hr());
			backgroundContextMenu.add(new Hr());
			backgroundContextMenu.add(createProgressEditor(gantt.getStepElement(targetStep.getUid())));
		});
	}

	private IntegerField createProgressEditor(StepElement stepElement) {
		var field = new IntegerField();
		field.setSuffixComponent(new Span("%"));
		field.setPlaceholder("Set progress");
		field.setStep(5);
		field.setStepButtonsVisible(true);
		field.setMin(0);
		field.setMax(100);
		// set initial value from first found progress bar component
		field.setValue(
				stepElement.getChildren().filter(ProgressBar.class::isInstance).findFirst().map(ProgressBar.class::cast)
						.map(progressBar -> progressBar.getValue()).map(Double::intValue).orElse(null));
		field.addValueChangeListener(ev -> {
			if (ev.getValue() > 0 && !stepElement.getChildren().anyMatch(ProgressBar.class::isInstance)) {
				stepElement.add(createProgressBar(0));
			}
			// updates step's all progress bar components
			stepElement.getChildren().filter(ProgressBar.class::isInstance).map(ProgressBar.class::cast)
					.forEach(progressBar -> {
						progressBar.setValue((ev.getValue()) % 101);
					});
		});
		return field;
	}

	private void onGanttStepClick(StepClickEvent event) {
		clickedBackgroundIndex = event.getIndex();
		String stepUid = event.getAnyStep().getUid();
		if (stepToStudyMap.containsKey(stepUid)) {
			Study study = stepToStudyMap.get(stepUid);
			StudyDetailsDialog dialog = new StudyDetailsDialog(study);
			dialog.open();
		} else if (stepToFieldworkMap.containsKey(stepUid)) {
			Fieldwork fieldwork = stepToFieldworkMap.get(stepUid);
			FieldworkDetailsDialog dialog = new FieldworkDetailsDialog(fieldwork);
			dialog.open();
		}
	}

	private Div buildControlPanel() {
		Div div = new Div();
		div.setWidthFull();
		HorizontalLayout tools = createTools();
		div.add(tools);
		return div;
	}

	private HorizontalLayout createTools() {
		HorizontalLayout tools = new HorizontalLayout();

		startDateField = new DatePicker(gantt.getStartDate());
		startDateField.setLabel("Inicio:");
		startDateField.addValueChangeListener(event -> gantt.setStartDate(event.getValue()));
		endDateField = new DatePicker(gantt.getEndDate());
		endDateField.setLabel("Fin:");
		endDateField.addValueChangeListener(event -> gantt.setEndDate(event.getValue()));

		typeFilterComboBox = new ComboBox<>("Tipo");
		List<String> typeOptions = new ArrayList<>();
		typeOptions.add(ALL_TYPES);
		Arrays.stream(FieldworkType.values()).map(Enum::name).forEach(typeOptions::add);
		typeFilterComboBox.setItems(typeOptions);
		typeFilterComboBox.setValue(ALL_TYPES);

		filterbutton = new Button("Filtrar");
		filterbutton.addClickListener(e -> {
			clearGantt();
			fillGantt();

		});

		zoomButton = new Button();
		updateZoomButton();
		zoomButton.addClickListener(e -> {
			dailyZoom = !dailyZoom;
			updateZoomButton();
			clearGantt();
			fillGantt();
		});

		tools.add(startDateField, endDateField, typeFilterComboBox, filterbutton, zoomButton);
		tools.setVerticalComponentAlignment(FlexComponent.Alignment.END, filterbutton, zoomButton);
		tools.setPadding(true);
		tools.setSpacing(true);
		return tools;
	}

	private void updateZoomButton() {
		if (dailyZoom) {
			zoomButton.setText("Ver casos por mes");
			zoomButton.setIcon(VaadinIcon.SEARCH_MINUS.create());
		} else {
			zoomButton.setText("Ver casos por día");
			zoomButton.setIcon(VaadinIcon.SEARCH_PLUS.create());
		}
	}

	private void clearGantt() {
		fieldworkStepsWithDailySubSteps.clear();
		this.gantt.removeSteps(gantt.getSteps());

	}

	enum SizeOption {
		FULL_SIZE("100% x 100%"), FULL_WIDTH("100% x auto"), HALF_WIDTH("50% x 100%"), HALF_HEIGHT("100% x 50%");

		private String text;

		private SizeOption(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}
}
