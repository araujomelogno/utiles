package uy.com.bay.utiles.security;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.auth.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Tag("div")
@Component
public class AccessDeniedExceptionHandler implements HasErrorParameter<AccessDeniedException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        Dialog dialog = new Dialog();
        dialog.add(new Text("No tiene permisos para acceder al módulo."));
        Button closeButton = new Button("Cerrar", e -> {
            dialog.close();
            UI.getCurrent().navigate("");
        });
        dialog.getFooter().add(closeButton);
        dialog.open();
        return HttpServletResponse.SC_FORBIDDEN;
    }
}
