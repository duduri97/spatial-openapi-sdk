package io.github.duduri97.spatial.apikey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public ApiKey apply(String applicationName, String ownerName, Integer dailyQuota) {
        int quota = (dailyQuota == null || dailyQuota <= 0) ? 1000 : dailyQuota;
        String keyValue = "spk_" + generateToken();
        return apiKeyRepository.save(new ApiKey(keyValue, applicationName, ownerName, ApiKey.Status.PENDING, quota));
    }

    @Transactional
    public ApiKey approve(Long id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("API key not found: " + id));
        key.approve();
        return key;
    }

    @Transactional
    public ApiKey revoke(Long id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("API key not found: " + id));
        key.revoke();
        return key;
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listAll() {
        return apiKeyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ApiKey findByKeyValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue).orElse(null);
    }

    private String generateToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
