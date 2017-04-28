package io.swagger.codegen.languages;

import com.eaglesakura.util.StringUtil;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

public class SwaggerGenerator extends DefaultCodegen implements CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerGenerator.class);

    public SwaggerGenerator() {
        super();
        embeddedTemplateDir = templateDir = "swagger";
        outputFolder = "generated-code/swagger";

//        supportingFiles.add(new SupportingFile("README.md", "", "README.md"));
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.DOCUMENTATION;
    }

    @Override
    public String getName() {
        return "swagger";
    }

    @Override
    public String getHelp() {
        return "Creates a static swagger.json file.";
    }

    @Override
    public void processSwagger(Swagger swagger) {

        // Fixed. Auto operationId
        for (Map.Entry<String, Path> pathEntry : swagger.getPaths().entrySet()) {
            String key = pathEntry.getKey();
            Path path = pathEntry.getValue();
            for (Map.Entry<HttpMethod, Operation> opEntry : path.getOperationMap().entrySet()) {
                HttpMethod method = opEntry.getKey();
                Operation op = opEntry.getValue();
                if (StringUtil.isEmpty(op.getOperationId())) {
                    // operationIdを埋める
                    op.setOperationId(getOrGenerateOperationId(op, key, method.name()));
                }
            }
        }

        String swaggerString = Json.pretty(swagger);

        try {
            String outputFile = outputFolder + File.separator + "swagger.json";
            FileUtils.writeStringToFile(new File(outputFile), swaggerString);
            LOGGER.debug("wrote file to " + outputFile);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public String escapeQuotationMark(String input) {
        // just return the original string
        return input;
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        // just return the original string
        return input;
    }
}
