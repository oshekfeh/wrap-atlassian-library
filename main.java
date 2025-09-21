public class Main {
    public static void main(String[] args) {
        String openApiSpecPath = "openapi.yaml";

        // Init schema extractor
        SchemaExtractor.init(openApiSpecPath);

        // Build Atlassian validator
        OpenApiInteractionValidator baseValidator =
                OpenApiInteractionValidator.createFor(openApiSpecPath).build();

        // Wrap with strict validator
        ResponseValidator validator = new StrictResponseValidator(baseValidator);

        // Validate a response
        Response response = com.atlassian.oai.validator.model.SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody("{\"id\":1,\"name\":\"test\"}")
                .build();

        Request request = com.atlassian.oai.validator.model.SimpleRequest.Builder
                .get("/users")
                .build();

        System.out.println(validator.validateResponse(response, request));
    }
}