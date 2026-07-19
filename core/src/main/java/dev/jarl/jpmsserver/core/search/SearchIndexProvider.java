package dev.jarl.jpmsserver.core.search;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public interface SearchIndexProvider {

    SearchIndex create(Map<String, String> config, ObjectMapper json);
}
