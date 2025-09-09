package uy.com.bay.utiles.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import javax.annotation.PostConstruct;

@Route("access-denied")
@AnonymousAllowed
public class AccessDeniedView extends Div {

    @PostConstruct
    private void showDialog() {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new H2("Acceso Denegado"));
        dialogLayout.add("No tiene permisos para acceder al módulo.");

        dialog.add(dialogLayout);

        Button closeButton = new Button("Cerrar", e -> {
            dialog.close();
            UI.getCurrent().navigate("");
        });

        dialog.getFooter().add(closeButton);

        dialog.open();
    }
}
