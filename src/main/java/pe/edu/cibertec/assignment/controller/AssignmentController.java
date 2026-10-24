package pe.edu.cibertec.assignment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.assignment.dto.common.ApiResponseDto;
import pe.edu.cibertec.assignment.dto.request.AssignmentRequestDto;
import pe.edu.cibertec.assignment.dto.response.AssignmentResponseDto;
import pe.edu.cibertec.assignment.entity.Assignment;
import pe.edu.cibertec.assignment.mapper.AssignmentMapper;
import pe.edu.cibertec.assignment.service.AssignmentService;

import java.util.List;

@RestController
@RequestMapping({"/api/assignments", "/api/v1/assignments"})
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentMapper assignmentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<AssignmentResponseDto> createAssignment(
            @Valid @RequestBody AssignmentRequestDto dto) {

        Assignment assignment = assignmentMapper.toEntity(dto);
        Assignment saved = assignmentService.createAssignment(assignment);

        return new ApiResponseDto<>(
                true,
                "Asignación registrada correctamente",
                assignmentMapper.toResponseDto(saved)
        );
    }

    @GetMapping
    public ApiResponseDto<List<AssignmentResponseDto>> getAllAssignments() {

        List<AssignmentResponseDto> assignments =
                assignmentService.getAllAssignments()
                        .stream()
                        .map(assignmentMapper::toResponseDto)
                        .toList();

        return new ApiResponseDto<>(
                true,
                "Asignaciones consultadas correctamente",
                assignments
        );
    }

    @GetMapping("/{id}")
    public ApiResponseDto<AssignmentResponseDto> getAssignmentById(
            @PathVariable Long id) {

        Assignment assignment = assignmentService.getAssignmentById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asignación no encontrada"
                ));

        return new ApiResponseDto<>(
                true,
                "Asignación encontrada",
                assignmentMapper.toResponseDto(assignment)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
    }

    @RequestMapping(value = "/{id}/return", method = {RequestMethod.POST, RequestMethod.PUT})
    public ApiResponseDto<AssignmentResponseDto> returnAssignment(
            @PathVariable Long id) {

        Assignment assignment = assignmentService.returnAssignment(id);

        return new ApiResponseDto<>(
                true,
                "Devolución registrada correctamente",
                assignmentMapper.toResponseDto(assignment)
        );
    }
}
