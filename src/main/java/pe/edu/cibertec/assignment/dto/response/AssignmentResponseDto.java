package pe.edu.cibertec.assignment.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AssignmentResponseDto {

    private Long id;
    private String assetId;
    private String userId;
    private LocalDate assignmentDate;
    private LocalDate returnDate;
    private String status;
}
