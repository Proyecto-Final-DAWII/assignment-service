package pe.edu.cibertec.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.assignment.entity.Assignment;
import pe.edu.cibertec.assignment.messaging.event.AuditEvent;
import pe.edu.cibertec.assignment.messaging.producer.AuditEventProducer;
import pe.edu.cibertec.assignment.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import feign.FeignException;
import pe.edu.cibertec.assignment.client.InventoryClient;
import pe.edu.cibertec.assignment.client.dto.InventoryResponseDto;
import pe.edu.cibertec.assignment.client.dto.InventoryStatusRequestDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AuditEventProducer auditEventProducer;
    private final InventoryClient inventoryClient;

    @Value("${app.inventory.validation.enabled:false}")
    private boolean inventoryValidationEnabled;

    @Transactional
    public Assignment createAssignment(Assignment assignment) {

        boolean yaAsignado = assignmentRepository
                .existsByAssetIdAndStatus(assignment.getAssetId(), "ASIGNADO");

        if (yaAsignado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El activo ya tiene una asignación activa"
            );
        }
        if (inventoryValidationEnabled) {
            validateInventoryAsset(assignment.getAssetId());
        }

        assignment.setStatus("ASIGNADO");
        assignment.setAssignmentDate(java.time.LocalDate.now());

        Assignment saved = assignmentRepository.save(assignment);
        if (inventoryValidationEnabled) {
            updateInventoryStatus(saved.getAssetId(), "ASIGNADO");
        }

        AuditEvent event = AuditEvent.builder()
                .assetId(saved.getAssetId())
                .userId(saved.getUserId())
                .actionType("ASIGNACION_CREADA")
                .previousState("DISPONIBLE")
                .newState("ASIGNADO")
                .details("Se registró una asignación")
                .eventDate(java.time.LocalDateTime.now())
                .build();

        auditEventProducer.publish(event);

        return saved;
    }
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    public void deleteAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asignación no encontrada"
                ));

        assignmentRepository.delete(assignment);
    }

    @Transactional
    public Assignment returnAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asignación no encontrada"
                ));
        if (!"ASIGNADO".equals(assignment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden devolver asignaciones activas"
            );
        }

        assignment.setReturnDate(java.time.LocalDate.now());
        assignment.setStatus("DEVUELTO");

        Assignment saved = assignmentRepository.save(assignment);
        if (inventoryValidationEnabled) {
            updateInventoryStatus(saved.getAssetId(), "DISPONIBLE");
        }

        AuditEvent event = AuditEvent.builder()
                .assetId(saved.getAssetId())
                .userId(saved.getUserId())
                .actionType("ASIGNACION_DEVUELTA")
                .previousState("ASIGNADO")
                .newState("DEVUELTO")
                .details("Se registró la devolución del activo")
                .eventDate(java.time.LocalDateTime.now())
                .build();

        auditEventProducer.publish(event);

        return saved;
    }
    private void validateInventoryAsset(String assetId) {
        InventoryResponseDto asset;

        try {
            asset = inventoryClient.getAssetById(assetId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El activo no existe en inventario"
            );
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar inventory-service"
            );
        }

        if (asset == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el activo en inventario"
            );
        }

        if (asset.getStatus() == null
                || !"DISPONIBLE".equalsIgnoreCase(asset.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El activo no está disponible para asignación"
            );
        }
    }

    private void updateInventoryStatus(String assetId, String status) {
        try {
            inventoryClient.updateStatus(assetId, new InventoryStatusRequestDto(status));
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo actualizar el estado en inventory-service"
            );
        }
    }
}
