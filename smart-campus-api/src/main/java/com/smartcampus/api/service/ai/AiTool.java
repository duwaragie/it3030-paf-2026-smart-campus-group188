package com.smartcampus.api.service.ai;

import tools.jackson.databind.node.ObjectNode;
import com.smartcampus.api.model.User;

public interface AiTool {

    String name();

    String description();

    ObjectNode parametersSchema();

    Object execute(ObjectNode arguments, User currentUser);

    /**
     * Controls whether this tool is advertised to the LLM for the given user.
     * Role-gated tools override this to cut per-request token cost \u2014 the LLM
     * never sees tools it cannot use. Defaults to available to every authenticated user.
     */
    default boolean isAvailableFor(User user) {
        return true;
    }
}
