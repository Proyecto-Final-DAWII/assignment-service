package pe.edu.cibertec.assignment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.cibertec.assignment.client.InventoryClient;
import pe.edu.cibertec.assignment.client.dto.InventoryResponseDto;
import pe.edu.cibertec.assignment.client.dto.InventoryStatusRequestDto;
import pe.edu.cibertec.assignment.entity.Assignment;
import pe.edu.cibertec.assignment.messaging.producer.AuditEventProducer;
import pe.edu.cibertec.assignment.repository.AssignmentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository repository;
    @Mock
    private AuditEventProducer producer;
    @Mock
    private InventoryClient inventoryClient;

    private AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(repository, producer, inventoryClient);
        ReflectionTestUtils.setField(service, "inventoryValidationEnabled", true);
    }

    @Test
    void assignsAnAvailableAssetAndUpdatesInventory() {
        Assignment assignment = new Assignment();
        assignment.setAssetId("LAP-001");
        assignment.setUserId("USR-001");
        InventoryResponseDto asset = new InventoryResponseDto();
        asset.setStatus("DISPONIBLE");
        when(inventoryClient.getAssetById("LAP-001")).thenReturn(asset);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Assignment saved = service.createAssignment(assignment);

        assertThat(saved.getStatus()).isEqualTo("ASIGNADO");
        verify(inventoryClient).updateStatus("LAP-001", new InventoryStatusRequestDto("ASIGNADO"));
    }
}
