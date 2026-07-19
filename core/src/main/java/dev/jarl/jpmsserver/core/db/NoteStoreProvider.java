package dev.jarl.jpmsserver.core.db;

import java.util.Map;

public interface NoteStoreProvider {

    NoteStore open(Map<String, String> config);
}
