package io.github.duduri97.spatial.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    public record ApplyKeyRequest(
            @NotBlank String applicationName,
            @NotBlank String ownerName,
            Integer dailyQuota
    ) {}

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKey apply(@Valid @RequestBody ApplyKeyRequest request) {
        return apiKeyService.apply(request.applicationName(), request.ownerName(), request.dailyQuota());
    }

    @PostMapping("/{id}/approve")
    public ApiKey approve(@PathVariable Long id) {
        return apiKeyService.approve(id);
    }

    @PostMapping("/{id}/revoke")
    public ApiKey revoke(@PathVariable Long id) {
        return apiKeyService.revoke(id);
    }

    @GetMapping
    public List<ApiKey> listAll() {
        return apiKeyService.listAll();
    }
}
