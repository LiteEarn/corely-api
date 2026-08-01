package br.com.corely.comercial.contractsnapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractSnapshotParser {

    private static final String NESTED_RULES_KEY = "rules";

    private final ObjectMapper objectMapper;

    public ContractSnapshotData parse(ContractSnapshot snapshot) {
        return parse(snapshot.getRules());
    }

    public ContractSnapshotData parse(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return ContractSnapshotData.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(rulesJson);
            JsonNode rulesNode = root;
            if (root.isObject() && root.has(NESTED_RULES_KEY) && root.get(NESTED_RULES_KEY).isObject()) {
                rulesNode = root.get(NESTED_RULES_KEY);
            }
            Map<String, Object> raw = objectMapper.convertValue(rulesNode, new TypeReference<>() {});
            return ContractSnapshotData.fromRules(normalizeKeys(raw));
        } catch (Exception e) {
            log.debug("Failed to parse contract snapshot rules: {}", e.getMessage());
            return ContractSnapshotData.empty();
        }
    }

    private Map<String, Object> normalizeKeys(Map<String, Object> raw) {
        Map<String, Object> normalized = new HashMap<>();
        raw.forEach((key, value) -> normalized.put(normalizeKey(key), value));
        return normalized;
    }

    private String normalizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
