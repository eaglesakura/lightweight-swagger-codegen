package com.eaglesakura.swagger.generator

import io.swagger.codegen.*
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.MapProperty
import io.swagger.models.properties.Property
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetrofitCodegen extends DefaultCodegen implements CodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetrofitCodegen.class)
    protected String invokerPackage = "io.swagger.client"
    protected String groupId = "io.swagger"
    protected String artifactId = "swagger-retrofit-client"
    protected String artifactVersion = "1.0.0"
    protected String projectFolder = ""
    protected String sourceFolder = ""
    protected String requestPackage = "io.swagger.client.request"
    protected String authPackage = "io.swagger.client.auth"
    protected String gradleWrapperPackage = "gradle.wrapper"

    RetrofitCodegen() {
        outputFolder = "generated-code/retrofit"
        modelTemplateFiles.put("model.mustache", ".kt")
        apiTemplateFiles.put("api.mustache", ".kt")
        embeddedTemplateDir = templateDir = "retrofit"
        apiPackage = "io.swagger.client.api"
        modelPackage = "io.swagger.client.model"

        setReservedWordsLowerCase([
                // local variable names used in API methods (endpoints)
                "localVarPostBody", "localVarPath", "localVarQueryParams", "localVarHeaderParams",
                "localVarFormParams", "localVarContentTypes", "localVarContentType",
                "localVarResponse", "localVarBuilder", "authNames", "basePath", "apiInvoker",

                // due to namespace collusion
                "Object",

                // java reserved words
                "abstract", "continue", "for", "new", "switch", "assert",
                "default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
                "this", "break", "double", "implements", "protected", "throw", "byte", "else",
                "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
                "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
                "native", "super", "while",
        ])

        languageSpecificPrimitives = new HashSet<String>([
                "String",
                "boolean",
                "Boolean",
                "Double",
                "Integer",
                "Long",
                "Float",
                "byte[]",
                "Object",
                "Any",
                "Unit"
        ])
        instantiationTypes.put("array", "ArrayList")
        instantiationTypes.put("map", "HashMap")
        typeMapping.put("date", "Date")
        typeMapping.put("file", "FileContent")

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC))
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC))
        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC))
    }


    @Override
    String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name)
        }
        return "_" + name
    }

    @Override
    String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.' as char, File.separatorChar)
    }

    @Override
    String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.' as char, File.separatorChar)
    }

    @Override
    String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace('/' as char, File.separatorChar)
    }

    @Override
    String modelDocFileFolder() {
        return (outputFolder + "/" + modelDocPath).replace('/' as char, File.separatorChar)
    }

    @Override
    String toApiDocFilename(String name) {
        return toApiName(name)
    }

    @Override
    String toModelDocFilename(String name) {
        return toModelName(name)
    }

    @Override
    String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p
            Property inner = ap.getItems()
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">"
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p
            Property inner = mp.getAdditionalProperties()

            return getSwaggerType(p) + "<String, " + getTypeDeclaration(inner) + ">"
        }
        return super.getTypeDeclaration(p)
    }

    @Override
    String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p)
        String type = null
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType)
            if (languageSpecificPrimitives.contains(type) || type.indexOf(".") >= 0 ||
                    type == "Map" || type == "List" ||
                    type == "File" || type == "Date") {
                return type
            }
        } else {
            type = swaggerType
        }
        return toModelName(type)
    }

    @Override
    String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name)
        // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_")
        // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all uppper case, do nothing
        if (name.matches('^[A-Z_]*$')) {
            return name
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = camelize(name, true)

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches('^\\d.*')) {
            name = escapeReservedWord(name)
        }

        return name
    }

    @Override
    String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name)
    }

    @Override
    String toModelName(String name) {
        // add prefix, suffix if needed
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix
        }

        // camelize the model name
        // phone_number => PhoneNumber
        name = camelize(sanitizeName(name))

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            String modelName = "Model" + name
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + modelName)
            return modelName
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            String modelName = "Model" + name
            // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName)
            return modelName
        }

        return name
    }

    @Override
    String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name)
    }

    @Override
    void setParameterExampleValue(CodegenParameter p) {
        String example

        if (p.defaultValue == null) {
            example = p.example
        } else {
            example = p.defaultValue
        }

        String type = p.baseType
        if (type == null) {
            type = p.dataType
        }

        if ("String" == type) {
            if (example == null) {
                example = p.paramName + "_example"
            }
            example = "\"" + escapeText(example) + "\""
        } else if ("Integer" == type || "Short" == type) {
            if (example == null) {
                example = "56"
            }
        } else if ("Long" == type) {
            if (example == null) {
                example = "56"
            }
            example = example + "L"
        } else if ("Float" == type) {
            if (example == null) {
                example = "3.4"
            }
            example = example + "F"
        } else if ("Double" == type) {
            example = "3.4"
            example = example + "D"
        } else if ("Boolean" == type) {
            if (example == null) {
                example = "true"
            }
        } else if ("File" == type) {
            if (example == null) {
                example = "/path/to/file"
            }
            example = "new File(\"" + escapeText(example) + "\")"
        } else if ("Date" == type) {
            example = "new Date()"
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()"
        }

        if (example == null) {
            example = "null"
        } else if (Boolean.TRUE == p.isListContainer) {
            example = "Arrays.asList(" + example + ")"
        } else if (Boolean.TRUE == p.isMapContainer) {
            example = "new HashMap()"
        }

        p.example = example
    }


    @Override
    String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed")
        }

        operationId = camelize(sanitizeName(operationId), true)

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true)
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId)
            return newOperationId
        }

        return operationId
    }

    @Override
    void processOpts() {
        super.processOpts()
        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER))
        }
        supportingFiles.add(new SupportingFile("api_factory.mustache",
                sourceFolder + "/" + apiPackage().replace('.' as char, File.separatorChar), "ApiFactory.kt"));
        supportingFiles.add(new SupportingFile("typeadapter.mustache",
                sourceFolder + "/" + modelPackage().replace('.' as char, File.separatorChar), "ApiEnumFactory.kt"));
    }


    Boolean getUseAndroidMavenGradlePlugin() {
        return useAndroidMavenGradlePlugin
    }

    void setUseAndroidMavenGradlePlugin(Boolean useAndroidMavenGradlePlugin) {
        this.useAndroidMavenGradlePlugin = useAndroidMavenGradlePlugin
    }

    void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage
    }

    void setGroupId(String groupId) {
        this.groupId = groupId
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId
    }

    void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion
    }

    void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder
    }

    void setSerializableModel(Boolean serializableModel) {
        this.serializableModel = serializableModel
    }

    @Override
    String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "")
    }

    @Override
    String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*")
    }

    @Override
    CodegenType getTag() {
        return CodegenType.CLIENT
    }

    @Override
    String getName() {
        return "Android Custom Client"
    }

    @Override
    String getHelp() {
        return "Android Custom Client generator"
    }
}
