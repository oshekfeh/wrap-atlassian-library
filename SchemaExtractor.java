import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SchemaExtractor {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static OpenAPI openAPI;

    public static void init(String openApiSpecPath) {
        openAPI = new OpenAPIV3Parser().read(openApiSpecPath);
        if (openAPI == null) {
            throw new IllegalStateException("Failed to load OpenAPI spec from: " + openApiSpecPath);
        }
    }

    public static String extractSchemaForResponse(Request request, Response response) throws Exception {
        if (openAPI == null) {
            throw new IllegalStateException("SchemaExtractor not initialized");
        }

        String path = request.getPath();
        String method = request.getMethod().toLowerCase();
        int status = response.getStatus();

        PathItem pathItem = openAPI.getPaths().get(path);
        if (pathItem == null) return null;

        Operation operation = getOperation(pathItem, method);
        if (operation == null) return null;

        ApiResponse apiResponse = operation.getResponses().get(String.valueOf(status));
        if (apiResponse == null || apiResponse.getContent() == null) return null;

        // Assume JSON response for now
        MediaType mediaType = apiResponse.getContent().get("application/json");
        if (mediaType == null) return null;

        Schema<?> schema = mediaType.getSchema();
        if (schema == null) return null;

        // Convert schema object to JSON string (for networknt validator)
        return mapper.writeValueAsString(schema);
    }

    private static Operation getOperation(PathItem pathItem, String method) {
        switch (method) {
            case "get": return pathItem.getGet();
            case "post": return pathItem.getPost();
            case "put": return pathItem.getPut();
            case "delete": return pathItem.getDelete();
            case "patch": return pathItem.getPatch();
            case "head": return pathItem.getHead();
            case "options": return pathItem.getOptions();
            case "trace": return pathItem.getTrace();
            default: return null;
        }
    }
}