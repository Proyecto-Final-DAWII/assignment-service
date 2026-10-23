package pe.edu.cibertec.assignment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.cibertec.assignment.config.openfeign.OpenFeignConfiguration;
import pe.edu.cibertec.assignment.client.dto.InventoryResponseDto;

@FeignClient(
        name = "inventory-service",
        configuration = OpenFeignConfiguration.class
)
public interface InventoryClient {

    @GetMapping("/api/inventory/{assetId}")
    InventoryResponseDto getAssetById(
            @PathVariable("assetId") String assetId
    );
}