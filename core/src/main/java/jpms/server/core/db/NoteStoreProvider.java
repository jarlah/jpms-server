package jpms.server.core.db;

import jpms.server.core.config.Config;

public interface NoteStoreProvider {

    NoteStore open(Config config);
}
