package pe.vallegrande.bullying.cases;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class CaseModels {
    private CaseModels() {}
    public record NewStudent(String nombres, String apellidos, Integer grado, String seccion) {}
    public record InvolvedInput(Long estudianteId, NewStudent nuevoEstudiante, String participacion) {}
    public record CreateCase(LocalDate fechaIncidente, String lugar, String tipoCodigo, String descripcion,
                             String observaciones, Long responsableId, List<InvolvedInput> estudiantes) {}
    public record Student(long id, String nombres, String apellidos, int grado, String seccion) {}
    public record Involved(Student estudiante, String participacion) {}
    public record Responsible(long id, String nombres, String apellidos, String rol) {}
    public record Catalog(String codigo, String nombre) {}
    public record Catalogs(List<Catalog> tipos, List<Catalog> estados, List<String> participaciones) {}
    public record Summary(long id, String codigo, LocalDate fechaIncidente, String lugar,
                          String tipoCodigo, String tipo, String estadoCodigo, String estado,
                          long responsableId, String responsable, long registradoPor, OffsetDateTime creadoEn) {}
    public record Detail(Summary caso, String descripcion, String observaciones, List<Involved> estudiantes) {}
    public record Page<T>(List<T> items, long total, int offset, int limit) {}
}
