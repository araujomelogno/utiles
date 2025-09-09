package uy.com.bay.utiles.security;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.auth.AccessDeniedException;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import org.springframework.stereotype.Component;

@Component
public class CustomViewAccessChecker extends ViewAccessChecker {

    public CustomViewAccessChecker() {
        super();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        try {
            super.beforeEnter(event);
        } catch (AccessDeniedException e) {
            showAccessDeniedDialog(event);
        }
    }

    private void showAccessDeniedDialog(BeforeEnterEvent event) {
        Dialog dialog = new Dialog();
        dialog.add(new Text("No tiene permisos para acceder al módulo."));
        Button closeButton = new Button("Cerrar", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.open();
        event.rerouteTo("");
    }
}
