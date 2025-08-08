package org.overb.jsontocsv.enums;

import lombok.Getter;

public enum CustomFunctions {
    CURRENT_TIMESTAMP("current_timestamp", 0),
    FIND("find", 3),
    CONCAT("concat", -1),
    JSON("json", 1),
    UNKNOWN("unknown", 0);

    @Getter
    private final String function;
    @Getter
    private final int parameters;

    CustomFunctions(String function, int parameters) {
        this.function = function;
        this.parameters = parameters;
    }

    public static CustomFunctions fromName(String functionName) {
        for (CustomFunctions customFunction : CustomFunctions.values()) {
            if (customFunction.function.equalsIgnoreCase(functionName)) {
                return customFunction;
            }
        }
        return UNKNOWN;
    }
}
