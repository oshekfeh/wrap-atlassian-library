import com.atlassian.oai.validator.interaction.response.ResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

public class StrictResponseValidator implements ResponseValidator {

    private final OpenApiInteractionValidator delegate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    public StrictResponseValidator(OpenApiInteractionValidator delegate) {
        this.delegate = delegate;
    }

    @Override
    public ValidationReport validateResponse(final Response response, final Request request) {
        // 1. Use Atlassian’s built-in validator
        ValidationReport report = delegate.validateResponse(request.getPath(), request.getMethod(), response);

        if (report.hasErrors()) {
            return report; // stop early if high-level validation already failed
        }

        // 2. Get schema JSON for this response (status + content type) from the OpenAPI spec
        try {
            String schemaJson = SchemaExtractor.extractSchemaForResponse(delegate, request, response);
            if (schemaJson != null) {
                JsonNode bodyNode = mapper.readTree(response.getBody().orElse("{}"));
                JsonSchema schema = schemaFactory.getSchema(schemaJson);

                Set<ValidationMessage> errors = schema.validate(bodyNode);
                if (!errors.isEmpty()) {
                    ValidationReport.Builder builder = ValidationReport.from(report);
                    errors.forEach(e -> builder.addMessage(ValidationReport.Message.create("schema", e.getMessage())));
                    return builder.build();
                }
            }
        } catch (Exception e) {
            ValidationReport.Builder builder = ValidationReport.from(report);
            builder.addMessage(ValidationReport.Message.create("schema", "Schema validation error: " + e.getMessage()));
            return builder.build();
        }

        return report;
    }
}