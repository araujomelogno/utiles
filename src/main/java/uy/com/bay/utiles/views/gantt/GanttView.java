package uy.com.bay.utiles.views.gantt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.element.StepElement;
import org.vaadin.tltv.gantt.event.GanttClickEvent;
import org.vaadin.tltv.gantt.event.StepClickEvent;
import org.vaadin.tltv.gantt.model.Resolution;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.dom.Style.Display;
import com.vaadin.flow.dom.Style.Position;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.GanttService;
import uy.com.bay.utiles.views.MainLayout;

@Route(value = "gantt", layout = MainLayout.class)
@PageTitle("Gantt")
@PermitAll
@Uses(Gantt.class)
public class GanttView extends VerticalLayout {

	private final GanttService ganttService;

	private TreeGrid<Step> treeGrid;
	private FlexLayout scrollWrapper;

	private DatePicker startDateField;
	private DatePicker endDateField;

	private int clickedBackgroundIndex;
	private LocalDateTime clickedBackgroundDate;
	private int stepCounter = 2;
	private Gantt gantt;

	private boolean addTreeData = true;

	public GanttView(GanttService ganttService) {
		this.ganttService = ganttService;
		setWidthFull();
		setPadding(false);

		gantt = createGantt();
		gantt.setWidth("70%");

		scrollWrapper = new FlexLayout();
		scrollWrapper.setId("scroll-wrapper");
		scrollWrapper.setMinHeight("0");
		scrollWrapper.setWidthFull();
		scrollWrapper.add(treeGrid, gantt);

		scrollWrapper.addComponentAsFirst(treeGrid);

		add(scrollWrapper);
	}

//	private void buildCaptionTreeGrid() {
//		treeGrid = gantt.buildCaptionTreeGrid("Header");
//		treeGrid.setWidth("30%");
//		treeGrid.setAllRowsVisible(true);
//		treeGrid.getStyle().set("--gantt-caption-grid-row-height", "40px");
//
//		Step parentStep = createDefaultNewStep();
//		gantt.addStep(parentStep);
//		Step childStep = createDefaultNewStep();
//		childStep.setUid(UUID.randomUUID().toString()); // needed when adding to TreeData directly
//		childStep.setCaption("Child step A");
//		childStep.setStartDate(parentStep.getStartDate());
//		childStep.setEndDate(parentStep.getStartDate().plusDays(3));
//		treeGrid.getTreeData().addItem(parentStep, childStep);
//		childStep = createDefaultNewStep();
//		childStep.setUid(UUID.randomUUID().toString());
//		childStep.setCaption("Child step B");
//		childStep.setStartDate(parentStep.getStartDate().plusDays(3));
//		childStep.setEndDate(parentStep.getStartDate().plusDays(7));
//		treeGrid.getTreeData().addItem(parentStep, childStep);
//		treeGrid.expand(parentStep);
//
//	}

	private Gantt createGantt() {
		Gantt gantt = new Gantt();
		gantt.setResolution(Resolution.Day);
		gantt.setStartDate(LocalDate.now().minusMonths(1l));
		gantt.setEndDate(LocalDate.now().plusMonths(6l));
		gantt.setLocale(UI.getCurrent().getLocale());
		gantt.setTimeZone(TimeZone.getDefault());
		gantt.setWidth("70%");
		treeGrid = gantt.buildCaptionTreeGrid("");
		treeGrid.setWidth("30%");
		treeGrid.setAllRowsVisible(true);
		treeGrid.getStyle().set("--gantt-caption-grid-row-height", "30px");

//		step1.setBackgroundColor("#9cfb84");
		treeGrid.addItemClickListener(this::onGanttTregridBackgroundClick);
		gantt.addGanttClickListener(this::onGanttBackgroundClick);

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

		List<Fieldwork> fieldworks = ganttService.getFieldworks();
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
			gantt.setMovableStepsBetweenRows(false);

			gantt.addStep(0, studyStep);

			fieldworkList.forEach(fieldwork -> {
				if (fieldwork.getInitPlannedDate() != null && fieldwork.getEndPlannedDate() != null) {
					Step subStep = new Step();
					subStep.setCaption(fieldwork.getType().toString());
					subStep.setStartDate(fieldwork.getInitPlannedDate().atStartOfDay());
					subStep.setEndDate(fieldwork.getEndPlannedDate().atStartOfDay());
					subStep.setUid(UUID.randomUUID().toString());
					subStep.setBackgroundColor("#E6E6E6");
					treeGrid.getTreeData().addItem(studyStep, subStep);

//					addDynamicSubStepContextMenu(gantt.getStepElement(subStep.getUid()));
					if (fieldwork.getGoalQuantity() != null && fieldwork.getGoalQuantity() > 0
							&& fieldwork.getCompleted() != null) {
						ProgressBar progressBar = new ProgressBar();
						double progress = (double) fieldwork.getCompleted() / fieldwork.getGoalQuantity();
						progressBar.setValue(progress);
						gantt.getStepElement(subStep.getUid()).add(progressBar);

					}

				}
			});
			treeGrid.expand(studyStep);
		});

		// Add dynamic context menu for gantt background. Clicked index is registered
		// via addGanttClickListener and addStepClickListener.
		addDynamicBackgroundContextMenu(gantt);

		return gantt;
	}

	private void addDynamicBackgroundContextMenu(Gantt gantt) {
		ContextMenu backgroundContextMenu = new ContextMenu();
		backgroundContextMenu.setTarget(gantt);
		gantt.getElement().addEventListener("vaadin-context-menu-before-open", event -> {
			backgroundContextMenu.removeAll();
			backgroundContextMenu.addItem("Add step at index " + clickedBackgroundIndex,
					e -> onHandleAddStepContextMenuAction(clickedBackgroundIndex, clickedBackgroundDate));
			var targetStep = gantt.getStepsList().get(clickedBackgroundIndex);
			backgroundContextMenu.addItem("Add sub-step for " + targetStep.getCaption(),
					e -> onHandleAddSubStepContextMenuAction(targetStep.getUid()));
			backgroundContextMenu.add(new Hr());
			backgroundContextMenu.addItem("Remove step " + targetStep.getCaption(),
					e -> onHandleRemoveStepContextMenuAction(targetStep.getUid()));
			if (gantt.getCaptionTreeGrid() != null) {
				backgroundContextMenu.add(new Hr());
				backgroundContextMenu.addItem("TreeGrid: Add new child step for " + targetStep.getCaption(),
						e -> onAddTreeGridChildStep(targetStep.getUid()));
			}
			backgroundContextMenu.add(new Hr());
			backgroundContextMenu.add(createProgressEditor(gantt.getStepElement(targetStep.getUid())));
		});
	}

	private void onAddTreeGridChildStep(String targetStepUid) {
		Step parentStep = gantt.getStep(targetStepUid);
		Step childStep = createDefaultNewStep();
		childStep.setUid(UUID.randomUUID().toString()); // needed when adding to TreeData directly
		childStep.setCaption(
				"Child Step " + (gantt.getCaptionTreeGrid().getTreeData().getChildren(parentStep).size() + 1));
		childStep.setStartDate(parentStep.getStartDate());
		childStep.setEndDate(parentStep.getStartDate().plusDays(7));
		gantt.getCaptionTreeGrid().getTreeData().addItem(parentStep, childStep);
		gantt.getCaptionTreeGrid().getDataProvider().refreshAll();

		if (!gantt.getCaptionTreeGrid().isExpanded(parentStep)) {
			treeGrid.expand(parentStep);
		} else {
			// Gantt can't know what was added/removed in data provider, so we need to call
			// expand to trigger expand event listener in Gantt.
			gantt.expand(parentStep, false);
		}
	}

	private void addDynamicSubStepContextMenu(StepElement stepElement) {
		stepElement.addContextMenu((contextMenu, uid) -> {
			contextMenu.removeAll();
			contextMenu.addItem("Add step at index " + clickedBackgroundIndex,
					e -> onHandleAddStepContextMenuAction(clickedBackgroundIndex, stepElement.getStartDateTime()));
			var targetStep = gantt.getStepsList().get(clickedBackgroundIndex);
			contextMenu.addItem("Add sub-step for " + targetStep.getCaption(),
					e -> onHandleAddSubStepContextMenuAction(targetStep.getUid()));
			contextMenu.add(new Hr());
			contextMenu.addItem("Remove step " + stepElement.getCaption(),
					e -> onHandleRemoveStepContextMenuAction(uid));
			contextMenu.add(new Hr());
			contextMenu.add(createProgressEditor(stepElement));
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

	private void onHandleRemoveStepContextMenuAction(String uid) {
		gantt.removeAnyStep(uid);
	}

	private void onHandleAddSubStepContextMenuAction(String uid) {
		var substep = createDefaultSubStep(uid);
		gantt.addSubStep(substep);
		addDynamicSubStepContextMenu(gantt.getStepElement(substep.getUid()));

	}

	private void onHandleAddStepContextMenuAction(int index, LocalDateTime startDate) {
		var step = createDefaultNewStep();
		if (startDate != null) {
			step.setStartDate(startDate);
			step.setEndDate(startDate.plusDays(7));
		}
		gantt.addStep(index, step);
	}

	private void onGanttBackgroundClick(GanttClickEvent event) {
		clickedBackgroundIndex = event.getIndex() != null ? event.getIndex() : 0;
		clickedBackgroundDate = event.getDate();
		if (event.getButton() == 2) {
			Notification.show("Clicked with mouse 2 at index: " + event.getIndex());
		} else {
			Notification.show("Clicked at index: " + event.getIndex() + " at date "
					+ event.getDate().format(DateTimeFormatter.ofPattern("M/d/yyyy HH:mm")));
		}
	}

	private void onGanttTregridBackgroundClick(ItemClickEvent event) {

		Notification.show("Clicked at index: " + event.getItem() + " at source " + event.getSource());

	}

	private void onGanttStepClick(StepClickEvent event) {
		clickedBackgroundIndex = event.getIndex();
		Notification.show("Clicked step " + event.getAnyStep().getCaption());
	}

	private Step createDefaultNewStep() {
		Step step = new Step();
		step.setCaption("New Step " + ++stepCounter);
		step.setBackgroundColor(String.format("#%06x", new Random().nextInt(0xffffff + 1)));
		step.setStartDate(LocalDateTime.of(2020, 4, 7, 0, 0));
		step.setEndDate(LocalDateTime.of(2020, 4, 14, 0, 0));
		return step;
	}

	private SubStep createDefaultSubStep(String ownerUid) {
		var owner = gantt.getStep(ownerUid);
		SubStep substep = new SubStep(owner);
		substep.setCaption("New Sub Step");
		substep.setBackgroundColor(String.format("#%06x", new Random().nextInt(0xffffff + 1)));
		if (gantt.getSubStepElements(ownerUid).count() == 0) {
			substep.setStartDate(owner.getStartDate());
			substep.setEndDate(owner.getEndDate());
		} else {
			substep.setStartDate(owner.getEndDate());
			substep.setEndDate(owner.getEndDate().plusDays(7));
			owner.setEndDate(substep.getEndDate());
			gantt.refresh(ownerUid);
		}
		return substep;
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

}
