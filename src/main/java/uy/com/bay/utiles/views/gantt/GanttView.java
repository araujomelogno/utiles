package uy.com.bay.utiles.views.gantt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.Uses;


import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.GanttService;
import uy.com.bay.utiles.views.MainLayout;

@Route(value = "gantt", layout = MainLayout.class)
@PageTitle("Gantt")
@PermitAll
@Uses(Gantt.class)
public class GanttView extends Div {

	private final GanttService ganttService;

	public GanttView(GanttService ganttService) {
		this.ganttService = ganttService;
		addClassName("gantt-view");
		setSizeFull();
		Gantt gantt = createGantt();
		add(gantt);
	}

	private Gantt createGantt() {
		Gantt gantt = new Gantt();
		gantt.setHeightFull();

		List<Fieldwork> fieldworks = ganttService.getFieldworks();
		Map<Study, List<Fieldwork>> fieldworksByStudy = fieldworks.stream()
				.collect(Collectors.groupingBy(Fieldwork::getStudy));

		fieldworksByStudy.forEach((study, fieldworkList) -> {
			Step studyStep = new Step();
			studyStep.setCaption(study.getName());

			fieldworkList.stream().map(Fieldwork::getInitPlannedDate).filter(Objects::nonNull).min(LocalDate::compareTo)
					.ifPresent(minDate -> studyStep.setStartDate(minDate.atStartOfDay()));
			fieldworkList.stream().map(Fieldwork::getEndPlannedDate).filter(Objects::nonNull).max(LocalDate::compareTo)
					.ifPresent(maxDate -> studyStep.setEndDate(maxDate.atStartOfDay()));

			gantt.addStep(studyStep);

			fieldworkList.forEach(fieldwork -> {
				if (fieldwork.getInitPlannedDate() != null && fieldwork.getEndPlannedDate() != null) {
					SubStep subStep = new SubStep(studyStep);
					subStep.setCaption(fieldwork.getType().toString());
					subStep.setStartDate(fieldwork.getInitPlannedDate().atStartOfDay());
					subStep.setEndDate(fieldwork.getEndPlannedDate().atStartOfDay());

					if (fieldwork.getGoalQuantity() != null && fieldwork.getGoalQuantity() > 0
							&& fieldwork.getCompleted() != null) {
						ProgressBar progressBar = new ProgressBar();
						double progress = (double) fieldwork.getCompleted() / fieldwork.getGoalQuantity();
						progressBar.setValue(progress);
						gantt.getStepElement(subStep.getUid()).add(progressBar);

					}
				}
			});
		});

		return gantt;
	}
}
