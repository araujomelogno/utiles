package uy.com.bay.utiles.views.gantt;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
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
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
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
	private final GanttService ganttService;
	private Gantt gantt;
	private FlexLayout scrollWrapper;
	private TreeGrid<Step> treeGrid;

	private DatePicker startDateField;
	private DatePicker endDateField;
	private Button filterbutton;
	private Double budgetTotal = 0d;
	private Double totalSpent = 0d;

	private int clickedBackgroundIndex;
	private int totalgoal = 0;
	private int totalCompleted = 0;
	private final Map<String, Fieldwork> stepToFieldworkMap;
	private final Map<String, Study> stepToStudyMap;

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

		treeGrid = new TreeGrid<>();
		treeGrid.addHierarchyColumn(Step::getCaption).setHeader("Proyecto");
		treeGrid.setWidth("30%");
		treeGrid.setAllRowsVisible(true);
		treeGrid.getStyle().set("--gantt-caption-grid-row-height", "30px");

		gantt.setMovableStepsBetweenRows(false);
		gantt.setMovableSteps(false);
		gantt.setResizableSteps(false);

		fillGantt();
	}

	private void fillGantt() {
		List<Fieldwork> fieldworks = ganttService.getFieldworks(gantt.getStartDate(), gantt.getEndDate());
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
					subStep.setCaption(fieldwork.getType().toString() + " - " + fieldwork.getGoalQuantity() + " casos");
					subStep.setStartDate(fieldwork.getInitPlannedDate().atStartOfDay());
					subStep.setEndDate(fieldwork.getEndPlannedDate().atStartOfDay());
					String uid = UUID.randomUUID().toString();
					subStep.setUid(uid);
					subStep.setBackgroundColor("#E6E6E6");
					subStep.setMovable(false);
					treeGrid.getTreeData().addItem(studyStep, subStep);
					stepToFieldworkMap.put(uid, fieldwork);
					if (fieldwork.getGoalQuantity() != null)
						totalgoal = totalgoal + fieldwork.getGoalQuantity();
					if (fieldwork.getCompleted() != null)
						totalCompleted = totalCompleted + fieldwork.getCompleted();

				}
			});

			totalSpent = 0d;
			budgetTotal = 0d;
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

		if (startDateField.getValue() != null && endDateField.getValue() != null)

		{
			if (startDateField.getValue().plusMonths(1).isBefore(endDateField.getValue())) {
				Step totalCasosCalleStep = new Step();
				totalCasosCalleStep.setCaption("Total Casos Calle");
				totalCasosCalleStep.setUid(UUID.randomUUID().toString());
				totalCasosCalleStep.setStartDate(startDateField.getValue().atStartOfDay());
				totalCasosCalleStep.setEndDate(endDateField.getValue().atStartOfDay());
//				totalCasosCalleStep.setBackgroundColor("#1E90FF");
				totalCasosCalleStep.setMovable(false);
				gantt.addStep(totalCasosCalleStep);

				LocalDate currentDate = startDateField.getValue();
				while (currentDate.isBefore(endDateField.getValue())) {
					LocalDate startOfMonth = currentDate.withDayOfMonth(1);
					LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
					int casosDelMes = fieldworks.stream()
							.filter(fw -> fw.getType() == FieldworkType.CALLE
									&& !fw.getInitPlannedDate().isAfter(endOfMonth)
									&& !fw.getEndPlannedDate().isBefore(startOfMonth))
							.mapToInt(Fieldwork::getGoalQuantity).sum();

					SubStep subStep = new SubStep(totalCasosCalleStep);
					subStep.setCaption("Casos del mes: " + casosDelMes);
					subStep.setStartDate(startOfMonth.atStartOfDay());
					subStep.setEndDate(endOfMonth.atStartOfDay().plusDays(1));
					subStep.setUid(UUID.randomUUID().toString());
					subStep.setBackgroundColor("#ADD8E6");
					subStep.setMovable(false);
					gantt.addSubStep(subStep);
//					treeGrid.getTreeData().addItem(totalCasosCalleStep, subStep);

					currentDate = currentDate.plusMonths(1);
				}
			}
		}
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

		filterbutton = new Button("Filtrar");
		filterbutton.addClickListener(e -> {
			clearGantt();
			fillGantt();

		});

		tools.add(startDateField, endDateField, filterbutton);
		tools.setVerticalComponentAlignment(FlexComponent.Alignment.END, filterbutton);
		tools.setPadding(true);
		tools.setSpacing(true);
		return tools;
	}

	private void clearGantt() {
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
