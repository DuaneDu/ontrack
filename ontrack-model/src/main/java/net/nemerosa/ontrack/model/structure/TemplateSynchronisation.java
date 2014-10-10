package net.nemerosa.ontrack.model.structure;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

/**
 * Synchronisation parameters for a {@linkplain net.nemerosa.ontrack.model.structure.TemplateDefinition template definition}.
 */
@Data
public class TemplateSynchronisation {

    /**
     * Source of branch names
     *
     * @see TemplateSynchronisationSource#getId()
     */
    private final String synchronisationSourceId;

    /**
     * Configuration data for the sync source
     */
    private final JsonNode synchronisationSourceConfig;

    /**
     * Policy to apply when a branch is configured but no longer available.
     */
    private final TemplateSynchronisationAbsencePolicy absencePolicy;

    /**
     * Synchronisation interval (in minutes). 0 means that synchronisation must be performed manually.
     */
    private final int interval;

    /**
     * Template expressions.
     */
    private final Map<String, String> templateExpressions;

}
