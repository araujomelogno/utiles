package uy.com.bay.utiles.views.answers;

import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import java.io.Serializable;
import lombok.Data;
import uy.com.bay.utiles.data.AlchemerAnswer;

@Data
public class AlchemerAnswerFilter implements Serializable {

    private GridLazyDataView<AlchemerAnswer> dataView;
    private String question = "";
    private String answer = "";
    private String type = "";

    public void setDataView(GridLazyDataView<AlchemerAnswer> dataView) {
        this.dataView = dataView;
    }

    public void setQuestion(String question) {
        this.question = question;
        dataView.refreshAll();
    }

    public void setAnswer(String answer) {
        this.answer = answer;
        dataView.refreshAll();
    }

    public void setType(String type) {
        this.type = type;
        dataView.refreshAll();
    }
}
