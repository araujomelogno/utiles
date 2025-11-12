package uy.com.bay.utiles.views.concepts;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

public class ConceptForm extends FormLayout {

    TextField name = new TextField("Nombre");
    TextArea description = new TextArea("Descripción");

    public ConceptForm() {
        add(name, description);
    }
}
