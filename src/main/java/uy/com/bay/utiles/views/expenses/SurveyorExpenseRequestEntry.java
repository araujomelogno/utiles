package uy.com.bay.utiles.views.expenses;

import java.util.Date;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Solicitar gasto")
@Route(value = "surveyor-expense-request", layout = MainLayout.class)
@RolesAllowed({ "ADMIN", "ENCUESTADORES" })
public class SurveyorExpenseRequestEntry extends Div {

	private ComboBox<Study> study;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private TextArea obs;
	private BeanValidationBinder<ExpenseRequest> binder;

	private final StudyService studyService;
	private final ExpenseRequestTypeService expenseRequestTypeService;
	private final ExpenseRequestService expenseRequestService;
	private final AuthenticatedUser authenticatedUser;
	private final SurveyorService surveyorService;

	public SurveyorExpenseRequestEntry(StudyService studyService, ExpenseRequestTypeService expenseRequestTypeService,
			ExpenseRequestService expenseRequestService, AuthenticatedUser authenticatedUser,
			SurveyorService surveyorService) {
		this.studyService = studyService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.expenseRequestService = expenseRequestService;
		this.authenticatedUser = authenticatedUser;
		this.surveyorService = surveyorService;

		addClassName("surveyor-expense-request-entry-view");

		FormLayout formLayout = new FormLayout();
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.findAllByShowSurveyor(true));
		study.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		study.setRequired(true);

		amount = new NumberField("Monto");
		amount.setRequiredIndicatorVisible(true);

		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(c -> c == null ? "" : c.getName());
		concept.setRequired(true);

		obs = new TextArea("Observaciones");

		Button saveButton = new Button("Guardar");
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		formLayout.add(study, amount, concept, obs, saveButton);
		add(formLayout);

		binder = new BeanValidationBinder<>(ExpenseRequest.class);
		binder.bindInstanceFields(this);

		saveButton.addClickListener(event -> {
			try {
				ExpenseRequest expenseRequest = new ExpenseRequest();
				binder.writeBean(expenseRequest);
				authenticatedUser.get().ifPresent(user -> {
					surveyorService.findByName(user.getUsername()).ifPresent(expenseRequest::setSurveyor);
				});
				expenseRequest.setRequestDate(new Date());
				expenseRequest.setExpenseStatus(ExpenseStatus.INGRESADO);

				expenseRequestService.update(expenseRequest);

				Notification.show("Solicitud enviada exitosamente", 3000, Notification.Position.BOTTOM_START);
				clearForm();
			} catch (ValidationException e) {
				Notification.show("Por favor, complete todos los campos requeridos.", 3000,
						Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
	}

	private void clearForm() {
		binder.readBean(new ExpenseRequest());
		obs.clear();
	}
}
