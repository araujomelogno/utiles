package uy.com.bay.utiles.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.stream.Stream;

@Component
@Primary
public class CustomViewAccessChecker extends AccessAnnotationChecker {

    @Override
    public boolean hasAccess(Class<?> view) {
        try {
            return super.hasAccess(view);
        } catch (AccessDeniedException e) {
            showAccessDeniedDialog();
            return false;
        }
    }

    @Override
    public boolean hasAccess(Method method) {
        try {
            return super.hasAccess(method);
        } catch (AccessDeniedException e) {
            showAccessDeniedDialog();
            return false;
        }
    }

    @Override
    public boolean hasAccess(Stream<Method> methods) {
        try {
            return super.hasAccess(methods);
        } catch (AccessDeniedException e) {
            showAccessDeniedDialog();
            return false;
        }
    }

    private void showAccessDeniedDialog() {
        UI.getCurrent().access(() -> {
            Dialog dialog = new Dialog();
            dialog.add(new Label("No tiene permisos para acceder a esta funcionalidad."));
            dialog.open();
        });
    }
}
