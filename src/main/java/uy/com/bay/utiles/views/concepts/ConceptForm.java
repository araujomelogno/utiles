package uy.com.bay.utiles.views.concepts;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import uy.com.bay.utiles.enums.ConceptType;

public class ConceptForm extends FormLayout {

    TextField name = new TextField("Nombre");
    TextArea description = new TextArea("Descripción");
    MultiSelectComboBox<ConceptType> type = new MultiSelectComboBox<>("Tipo");

    public ConceptForm() {
        type.setItems(ConceptType.values());
        type.setItemLabelGenerator(ConceptType::name);
        add(name, description, type);
    }
}
