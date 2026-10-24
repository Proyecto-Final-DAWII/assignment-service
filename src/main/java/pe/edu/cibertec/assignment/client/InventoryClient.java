package pe.edu.cibertec.assignment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.edu.cibertec.assignment.config.openfeign.OpenFeignConfiguration;
import pe.edu.cibertec.assignment.client.dto.InventoryResponseDto;
import pe.edu.cibertec.assignment.client.dto.InventoryStatusRequestDto;

@FeignClient(
        name = "inventory-service",
        configuration = OpenFeignConfiguration.class
)
public interface InventoryClient {

    @GetMapping("/api/inventory/{assetId}")
    InventoryResponseDto getAssetById(
            @PathVariable("assetId") String assetId
    );

    @PutMapping("/api/inventory/{assetId}/status")
    InventoryResponseDto updateStatus(
            @PathVariable("assetId") String assetId,
            @RequestBody InventoryStatusRequestDto request
    );
}
