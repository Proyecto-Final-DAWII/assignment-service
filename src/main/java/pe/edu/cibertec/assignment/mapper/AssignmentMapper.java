package pe.edu.cibertec.assignment.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cibertec.assignment.dto.request.AssignmentRequestDto;
import pe.edu.cibertec.assignment.dto.response.AssignmentResponseDto;
import pe.edu.cibertec.assignment.entity.Assignment;

@Component
public class AssignmentMapper {

    public Assignment toEntity(AssignmentRequestDto dto) {
        Assignment assignment = new Assignment();
        assignment.setAssetId(dto.getAssetId());
        assignment.setUserId(dto.getUserId());
        return assignment;
    }

    public AssignmentResponseDto toResponseDto(Assignment assignment) {
        AssignmentResponseDto dto = new AssignmentResponseDto();
        dto.setId(assignment.getId());
        dto.setAssetId(assignment.getAssetId());
        dto.setUserId(assignment.getUserId());
        dto.setAssignmentDate(assignment.getAssignmentDate());
        dto.setReturnDate(assignment.getReturnDate());
        dto.setStatus(assignment.getStatus());
        return dto;
    }
}