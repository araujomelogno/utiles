package uy.com.bay.utiles.views.whatsapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.dto.whatsapp.WhatsappTemplate;
import uy.com.bay.utiles.services.WhatsappService;
import uy.com.bay.utiles.views.MainLayout;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@PageTitle("Enviar mensajes")
@Route(value = "whatsapp-sender", layout = MainLayout.class)
@PermitAll
public class WhatsappSenderView extends VerticalLayout {

    private final WhatsappService whatsappService;

    private ComboBox<WhatsappTemplate> templateComboBox;
    private DateTimePicker dateTimePicker;
    private Checkbox headerCheckbox;
    private Upload upload;
    private MemoryBuffer buffer;
    private Button processButton;
    private boolean fileUploaded = false;

    public WhatsappSenderView(WhatsappService whatsappService) {
        this.whatsappService = whatsappService;

        setWidthFull();
        setSpacing(true);
        setPadding(true);

        createStep1();
        createStep2();
        createStep3();
        createProcessButton();
    }

    private void createStep1() {
        add(new H3("1-Seleccionar template de Meta - Flow"));

        templateComboBox = new ComboBox<>("Template");
        templateComboBox.setItemLabelGenerator(WhatsappTemplate::getName);
        templateComboBox.setItems(whatsappService.getTemplates());
        templateComboBox.setWidth("300px");

        add(templateComboBox);
    }

    private void createStep2() {
        add(new H3("2-Seleccionar fecha y hora de envío"));

        dateTimePicker = new DateTimePicker("Fecha y Hora");
        dateTimePicker.setValue(LocalDateTime.now());

        add(dateTimePicker);
    }

    private void createStep3() {
        add(new H3("3-Cargar base de números (Un excel , la primera columna es el celular y deben comenzar con 598xxx. Las siguientes columnas los parámetros )"));

        headerCheckbox = new Checkbox("La primera fila tiene cabezales");

        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setMaxFiles(1);

        upload.addSucceededListener(event -> {
            fileUploaded = true;
            Notification.show("Archivo subido: " + event.getFileName());
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("Archivo rechazado: " + event.getErrorMessage());
        });

        add(headerCheckbox, upload);
    }

    private void createProcessButton() {
        processButton = new Button("Procesar");
        processButton.addClickListener(e -> process());
        add(processButton);
    }

    private void process() {
        if (templateComboBox.getValue() == null) {
            Notification.show("Debe seleccionar un template");
            return;
        }

        if (!fileUploaded) {
             Notification.show("Debe subir un archivo");
             return;
        }

        try {
            InputStream inputStream = buffer.getInputStream();
            if (inputStream == null) {
                 Notification.show("Error al leer el archivo");
                 return;
            }

            LocalDateTime ldt = dateTimePicker.getValue();
            Date schedule = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());

            InputStream processedStream = whatsappService.processExcel(inputStream, headerCheckbox.getValue(), templateComboBox.getValue(), schedule);

            StreamResource resource = new StreamResource("procesado.xlsx", () -> processedStream);
            Anchor download = new Anchor(resource, "Descargar archivo procesado");
            download.getElement().setAttribute("download", true);

            add(download);
            Notification.show("Archivo procesado");

        } catch (Exception ex) {
            Notification.show("Error al procesar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
