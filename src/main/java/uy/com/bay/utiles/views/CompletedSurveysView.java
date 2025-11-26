package uy.com.bay.utiles.views;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.addons.componentfactory.monthpicker.MonthPicker;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;
import uy.com.bay.utiles.data.User;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;
import uy.com.bay.utiles.dto.CompletedSurveyDTO;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.util.ExcelExporter;

@PageTitle("Encuestas Completas")
@Route(value = "completed-surveys", layout = MainLayout.class)
@PermitAll
public class CompletedSurveysView extends VerticalLayout {

	private final AlchemerAnswerRepository alchemerAnswerRepository;
	private final SurveyorRepository surveyorRepository;

	private final MultiSelectComboBox<Surveyor> surveyorComboBox = new MultiSelectComboBox<>("Encuestador");
	private final MonthPicker monthPicker = new MonthPicker();
	private final Button exportButton = new Button("Exportar", VaadinIcon.DOWNLOAD.create());
	private final Grid<CompletedSurveyDTO> grid = new Grid<>(CompletedSurveyDTO.class);
	private List<CompletedSurveyDTO> results = new ArrayList<>();

	public CompletedSurveysView(AlchemerAnswerRepository alchemerAnswerRepository,
			SurveyorRepository surveyorRepository, AuthenticatedUser authenticatedUser) {
		this.alchemerAnswerRepository = alchemerAnswerRepository;
		this.surveyorRepository = surveyorRepository;

		setSizeFull();
		setSpacing(false);
		setAlignItems(Alignment.CENTER);

		surveyorComboBox.setItems(surveyorRepository.findAll());
		surveyorComboBox.setItemLabelGenerator(Surveyor::getName);
		monthPicker.setValue(YearMonth.now());

		Optional<User> maybeUser = authenticatedUser.get();
		if (maybeUser.isPresent()) {
			User user = maybeUser.get();
			if (user.getRoles().stream().anyMatch(role -> role.name().equals("ENCUESTADORES"))) {
				surveyorRepository.findByLogin(user.getUsername()).ifPresent(surveyor -> {
					surveyorComboBox.setValue(Set.of(surveyor));
					surveyorComboBox.setVisible(false);
				});
			}
		}

		surveyorComboBox.addValueChangeListener(event -> fetchData());
		monthPicker.addValueChangeListener(event -> fetchData());

		HorizontalLayout filtersLayout = new HorizontalLayout(surveyorComboBox, monthPicker, exportButton);
		filtersLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
		filtersLayout.setSpacing(true);

		grid.removeAllColumns();
		grid.addColumn(CompletedSurveyDTO::getSurveyor).setHeader("Encuestador");
		grid.addColumn(CompletedSurveyDTO::getStudyName).setHeader("Estudio");
		grid.addColumn(new LocalDateRenderer<>(CompletedSurveyDTO::getCreated,
				() -> DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setHeader("Fecha");
		grid.addColumn(CompletedSurveyDTO::getCount).setHeader("Cantidad");

		exportButton.addClickListener(e -> exportToExcel());

		add(filtersLayout, grid);
		fetchData();
	}

	private void exportToExcel() {
		try {
			StreamResource sr = new StreamResource("completed-surveys.xlsx", () -> {
				try {
					return ExcelExporter.exportToExcel(results);
				} catch (IOException e) {
					Notification.show("Error al generar el archivo Excel.", 3000, Notification.Position.TOP_CENTER);
					return null;
				}
			});
			Anchor anchor = new Anchor(sr, "");
			anchor.getElement().setAttribute("download", true);
			anchor.getStyle().set("display", "none");
			add(anchor);
			anchor.getElement().callJsFunction("click");

		} catch (Exception e) {
			Notification.show("Error al exportar a Excel.", 3000, Notification.Position.TOP_CENTER);
		}
	}

	private void fetchData() {
		if (surveyorComboBox.getValue().isEmpty() || monthPicker.getValue() == null) {
			grid.setItems(new ArrayList<>());
			return;
		}

		List<String> selectedSurveyors = surveyorComboBox.getValue().stream().map(Surveyor::getLogin)
				.collect(Collectors.toList());
		YearMonth selectedMonth = monthPicker.getValue();
		LocalDate startDate = selectedMonth.atDay(1);
		LocalDate endDate = selectedMonth.atEndOfMonth();

		results = alchemerAnswerRepository.findCompletedSurveys(selectedSurveyors, startDate, endDate);
		grid.setItems(results);
	}
}
