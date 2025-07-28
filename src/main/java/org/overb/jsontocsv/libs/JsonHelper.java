package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonHelper {

    public static int maxDepth(JsonNode node) {
        if (!node.isContainerNode()) {
            return 1;
        }
        int maxChild = 0;
        if (node.isObject()) {
            for (JsonNode child : node) {
                maxChild = Math.max(maxChild, maxDepth(child));
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maxChild = Math.max(maxChild, maxDepth(element));
            }
        }
        return maxChild;
    }

    public static boolean isNested(JsonNode node) {
        return maxDepth(node) > 1;
    }
}
