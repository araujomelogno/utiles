package uy.com.bay.utiles.repo;

import java.time.LocalDate;
import java.util.List;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.entities.Extra;

public interface ExtraRepositoryCustom {
    List<Extra> findExtrasByFilters(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<Surveyor> encuestadores,
            List<Study> estudios
    );
}