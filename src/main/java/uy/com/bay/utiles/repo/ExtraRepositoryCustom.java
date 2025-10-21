package uy.com.bay.utiles.repo;

import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.entities.Extra;

import java.time.LocalDate;
import java.util.List;

public interface ExtraRepositoryCustom {
    List<Extra> findExtrasByFilters(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<Surveyor> encuestadores,
            List<Study> estudios,
            List<ExtraConcept> conceptos
    );
}