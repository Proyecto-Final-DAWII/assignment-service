package pe.edu.cibertec.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentRequestDto {

    @NotBlank
    private String assetId;
    @NotBlank
    private String userId;
}