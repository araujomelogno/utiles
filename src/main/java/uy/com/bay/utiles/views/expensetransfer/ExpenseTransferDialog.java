package uy.com.bay.utiles.views.expensetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;

import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import uy.com.bay.utiles.data.Surveyor;

public class ExpenseTransferDialog extends Dialog {

	private DatePicker transferDate;
	private NumberField amount;
	private TextArea obs;
	private Upload upload;
	protected MultiFileMemoryBuffer buffer;

	private Button saveButton;
	private Button cancelButton;

	private BeanValidationBinder<ExpenseTransfer> binder;

	protected ExpenseTransfer expenseTransfer;
	protected Set<ExpenseRequest> selectedRequests;

	public ExpenseTransferDialog(Set<ExpenseRequest> selectedRequests) {
		this.selectedRequests = selectedRequests;
		this.expenseTransfer = new ExpenseTransfer();

		setHeaderTitle("Crear Transferencia");

		FormLayout formLayout = new FormLayout();
		transferDate = new DatePicker("Fecha de Transferencia");
		transferDate.setValue(LocalDate.now());

		amount = new NumberField("Monto");

		double totalAmount = selectedRequests.stream().mapToDouble(er -> er.getAmount() != null ? er.getAmount() : 0)
				.sum();
		
		amount.setValue(totalAmount);
		amount.setReadOnly(true);
		obs = new TextArea("Observaciones");

		buffer = new MultiFileMemoryBuffer();
		upload = new Upload(buffer);
		upload.setAcceptedFileTypes("image/jpeg", "image/png", "application/pdf", ".doc", ".docx", ".xls", ".xlsx");
		upload.setMaxFiles(5);
		upload.setMaxFileSize(20 * 1024 * 1024); // 20MB

		formLayout.add(transferDate, amount, obs, upload);
		add(formLayout);

		createButtons();
		getFooter().add(new HorizontalLayout(saveButton, cancelButton));

		binder = new BeanValidationBinder<>(ExpenseTransfer.class);
		binder.bind(amount, "amount");
		binder.bind(obs, "obs");

		expenseTransfer.setAmount(totalAmount);
		binder.readBean(expenseTransfer);

	}

	private void createButtons() {
		saveButton = new Button("Guardar", e -> save());
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		cancelButton = new Button("Cancelar", e -> close());
	}

	private void save() {
		try {
			if (selectedRequests.isEmpty()) {
				Notification.show("No hay solicitudes de gastos seleccionadas.");
				return;
			}
			Surveyor firstSurveyor = selectedRequests.iterator().next().getSurveyor();
			if (firstSurveyor == null) {
				Notification.show("La solicitud de gasto seleccionada no tiene un encuestador asignado.", 3000,
						Position.MIDDLE);
				return;
			}
			for (ExpenseRequest request : selectedRequests) {
				if (!firstSurveyor.equals(request.getSurveyor())) {
					Notification.show("Solo se pueden crear transferencias vinculando a un mismo encuestador", 3000,
							Position.MIDDLE);

					return;
				}
			}

			if (binder.writeBeanIfValid(expenseTransfer)) {
				expenseTransfer.setSurveyor(firstSurveyor);
				LocalDate localDate = transferDate.getValue();
				if (localDate != null) {
					Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
					expenseTransfer.setTransferDate(date);
				}

				List<ExpenseTransferFile> files = new ArrayList<>();
				for (String fileName : buffer.getFiles()) {
					InputStream inputStream = buffer.getInputStream(fileName);
					byte[] content = inputStream.readAllBytes();

					ExpenseTransferFile file = new ExpenseTransferFile();
					file.setName(fileName);
					file.setCreated(new Date());
					file.setContent(content);
					file.setExpenseTransfer(expenseTransfer);
					files.add(file);
				}
				expenseTransfer.setFiles(files);

				List<ExpenseRequest> requestList = new ArrayList<>(selectedRequests);
				expenseTransfer.setExpenseRequests(requestList);

				for (ExpenseRequest request : requestList) {
					request.setExpenseTransfer(expenseTransfer);
				}

				fireEvent(new SaveEvent(this, expenseTransfer));
				close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Notification.show("Error al guardar el archivo.");
		}
	}

	// Events
	public static abstract class ExpenseTransferDialogEvent extends ComponentEvent<ExpenseTransferDialog> {
		private final ExpenseTransfer expenseTransfer;

		protected ExpenseTransferDialogEvent(ExpenseTransferDialog source, ExpenseTransfer expenseTransfer) {
			super(source, false);
			this.expenseTransfer = expenseTransfer;
		}

		public ExpenseTransfer getExpenseTransfer() {
			return expenseTransfer;
		}
	}

	public static class SaveEvent extends ExpenseTransferDialogEvent {
		SaveEvent(ExpenseTransferDialog source, ExpenseTransfer expenseTransfer) {
			super(source, expenseTransfer);
		}
	}

	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
			ComponentEventListener<T> listener) {
		return getEventBus().addListener(eventType, listener);
	}
}
