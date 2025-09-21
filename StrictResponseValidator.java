import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.interaction.response.ResponseValidator;  // Atlassian class
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.Set;

public class StrictResponseValidator {

    private final ResponseValidator delegate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    public StrictResponseValidator(OpenApiInteractionValidator interactionValidator, OpenAPI openApiSpec) {
        // Assumes Atlassian’s ResponseValidator has a constructor which takes OpenApiInteractionValidator and possibly the spec
        // Or if it's accessible via factory methods — adjust accordingly.
        this.delegate = new ResponseValidator(interactionValidator);
        SchemaExtractor.init(openApiSpec);
    }

    /**
     * Strict validation combining Atlassian’s and JSON Schema.
     */
    public ValidationReport validate(Response response, Request request) {
        // 1. Use Atlassian’s validation first
        ValidationReport report = delegate.validate(response, request);
        if (report.hasErrors()) {
            return report;
        }

        // 2. Strict schema-level validation
        try {
            String schemaJson = SchemaExtractor.extractSchemaForResponse(request, response);
            if (schemaJson != null && response.getBody().isPresent()) {
                JsonNode bodyNode = mapper.readTree(response.getBody().get());
                JsonSchema schema = schemaFactory.getSchema(schemaJson);
                Set<ValidationMessage> errors = schema.validate(bodyNode);

                if (!errors.isEmpty()) {
                    // Build a new report that includes schema errors
                    ValidationReport.Builder builder = ValidationReport.from(report);
                    for (ValidationMessage err : errors) {
                        builder.addMessage(
                            ValidationReport.Message.create("schema", err.getMessage())
                        );
                    }
                    return builder.build();
                }
            }
        } catch (Exception e) {
            ValidationReport.Builder builder = ValidationReport.from(report);
            builder.addMessage(
                ValidationReport.Message.create(
                    "schema",
                    "Error during strict schema validation: " + e.getMessage()
                )
            );
            return builder.build();
        }

        // All good
        return report;
    }
}