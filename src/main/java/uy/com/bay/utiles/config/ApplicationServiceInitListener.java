package uy.com.bay.utiles.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uy.com.bay.utiles.security.CustomViewAccessChecker;

@Component
public class ApplicationServiceInitListener implements VaadinServiceInitListener {

    @Autowired
    private CustomViewAccessChecker accessChecker;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(accessChecker);
        });
    }
}
