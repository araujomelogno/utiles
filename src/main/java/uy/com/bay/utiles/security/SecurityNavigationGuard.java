package uy.com.bay.utiles.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;

@Component
public class SecurityNavigationGuard implements VaadinServiceInitListener {

	@Autowired
	private AccessAnnotationChecker accessChecker;

	@Override
	public void serviceInit(ServiceInitEvent event) {
		event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI().addBeforeEnterListener(this::checkAccess));
	}

	private void checkAccess(BeforeEnterEvent event) {
		Class<?> target = event.getNavigationTarget();

		if (!accessChecker.hasAccess(target)) {
			UI ui = event.getUI();
			ui.access(() -> {
				Dialog dialog = new Dialog();
				dialog.setHeaderTitle("Acceso denegado");
				dialog.add(new Paragraph("No tenés permisos para acceder a esta sección. "
						+ "Si creés que es un error, contactá al administrador."));
				dialog.getFooter().add(new Button("Cerrar", e -> dialog.close()));
				dialog.open();
			});

			// Cambiá HomeView por tu vista segura (ruta pública/logueado sin rol)
			event.rerouteTo("login");
		}
	}
}