package uy.com.bay.utiles.views.areas;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Area;
import uy.com.bay.utiles.services.AreaService;

@PageTitle("Areas")
@Route(value = "areas/:areaID?/:action?(edit)")
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class AreasView extends Div implements BeforeEnterObserver {

	private final String AREA_ID = "areaID";
	private final String AREA_EDIT_ROUTE_TEMPLATE = "areas/%s/edit";

	private final Grid<Area> grid = new Grid<>(Area.class, false);

	private TextField nombre;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Eliminar");

	private final BeanValidationBinder<Area> binder;

	private Area area;

	private final AreaService areaService;

	public AreasView(AreaService areaService) {
		this.areaService = areaService;
		addClassNames("areas-view");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid
		grid.addColumn("nombre").setAutoWidth(true);
		grid.setItems(query -> areaService.list(PageRequest.of(query.getPage(), query.getPageSize())).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(AREA_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(AreasView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Area.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.area == null) {
					this.area = new Area();
				}
				binder.writeBean(this.area);
				areaService.save(this.area);
				clearForm();
				refreshGrid();
				Notification.show("Area guardada.");
				UI.getCurrent().navigate(AreasView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show("Error al guardar el area. Alguien mas la ha modificado.");
				n.setPosition(Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Fallo al guardar el area. Verifique que todos los valores son validos.");
			}
		});

		delete.addClickListener(e -> {
			if (this.area != null && this.area.getId() != null) {
				try {
					areaService.delete(this.area.getId());
					clearForm();
					refreshGrid();
					Notification.show("Area eliminada.");
					UI.getCurrent().navigate(AreasView.class);
				} catch (Exception ex) {
					Notification
							.show("Error al eliminar el area: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
							.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<String> areaIdParam = event.getRouteParameters().get(AREA_ID);
		if (areaIdParam.isPresent()) {
			String areaId = areaIdParam.get();
			if ("new".equals(areaId)) {
				clearForm();
				grid.asSingleSelect().clear();
			} else {
				try {
					Optional<Area> areaFromBackend = areaService.get(Long.parseLong(areaId));
					if (areaFromBackend.isPresent()) {
						populateForm(areaFromBackend.get());
					} else {
						Notification.show(String.format("El area con id = '%s' no fue encontrada", areaId), 3000,
								Notification.Position.BOTTOM_START);
						refreshGrid();
						event.forwardTo(AreasView.class);
					}
				} catch (NumberFormatException e) {
					Notification.show(String.format("El id de area debe ser un numero. Id recibido: '%s'", areaId), 3000,
							Notification.Position.BOTTOM_START);
					refreshGrid();
					event.forwardTo(AreasView.class);
				}
			}
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		Div editorLayoutDiv = new Div();
		editorLayoutDiv.setClassName("editor-layout");

		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		nombre = new TextField("Nombre");
		formLayout.add(nombre);

		editorDiv.add(formLayout);
		createButtonLayout(editorLayoutDiv);

		splitLayout.addToSecondary(editorLayoutDiv);
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		buttonLayout.add(save, cancel, delete);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");

		Button createAreaButton = new Button("Crear Area");
		createAreaButton.addClickListener(e -> {
			clearForm();
			UI.getCurrent().navigate(String.format(AREA_EDIT_ROUTE_TEMPLATE, "new"));
		});

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.add(createAreaButton);

		wrapper.add(topLayout, grid);
		splitLayout.addToPrimary(wrapper);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(Area value) {
		this.area = value;
		binder.readBean(this.area);
	}
}
