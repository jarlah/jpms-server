package dev.jarl.jpmsserver.core.search;

import tools.jackson.databind.node.ObjectNode;

public sealed interface BulkOp {

    record Index(String index, String id, ObjectNode doc) implements BulkOp {}

    record Delete(String index, String id) implements BulkOp {}
}
