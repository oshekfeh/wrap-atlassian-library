import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class SchemaExtractor {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static OpenAPI openAPI;

    public static void init(String openApiSpecPath) {
        openAPI = new OpenAPIV3Parser().read(openApiSpecPath);
    }

    public static String extractSchemaForResponse(OpenApiInteractionValidator validator,
                                                  Request request,
                                                  Response response) throws Exception {
        if (openAPI == null) {
            throw new IllegalStateException("SchemaExtractor not initialized");
        }

        String path = request.getPath();
        String method = request.getMethod().toLowerCase();
        int status = response.getStatus();

        ApiResponse apiResponse = openAPI.getPaths()
                .get(path)
                .getOperationMap()
                .get(io.swagger.v3.oas.models.PathItem.HttpMethod.valueOf(method.toUpperCase()))
                .getResponses()
                .get(String.valueOf(status));

        if (apiResponse == null) return null;

        // Assuming application/json only
        Schema<?> schema = apiResponse.getContent()
                .get("application/json")
                .getSchema();

        if (schema == null) return null;

        return mapper.writeValueAsString(schema);
    }
}