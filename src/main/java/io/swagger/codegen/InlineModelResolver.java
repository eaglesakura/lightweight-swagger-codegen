package io.swagger.codegen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Xml;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.AbstractProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;

public class InlineModelResolver {
    private Swagger swagger;
    private boolean skipMatches;
    static Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);

    Map<String, Model> addedModels = new HashMap<String, Model>();
    Map<String, String> generatedSignature = new HashMap<String, String>();

    /**
     * internal model
     */
    protected String toInlineName(String path, HttpMethod method, Operation op, Parameter parameter, String key) {
        StringBuilder builder = new StringBuilder();

        builder.append(path).append("_").append(method.name());
        if (parameter != null) {
            // input parameterである
//            builder.append("_").append(!StringUtil.isEmpty(parameter.getIn()) ? parameter.getIn() : key);
            builder.append("_").append(key);
        } else {
            builder.append("_").append(key);
        }
        String temp = builder.toString();
        temp = temp.replaceAll("\\/", "_").replaceAll("\\{", "").replaceAll("}", "");
        if (temp.startsWith("_")) {
            temp = temp.substring(1);
        }
        return temp.toLowerCase();
    }

    public void flatten(Swagger swagger) {
        this.swagger = swagger;

        if (swagger.getDefinitions() == null) {
            swagger.setDefinitions(new HashMap<String, Model>());
        }

        // operations
        Map<String, Path> paths = swagger.getPaths();
        Map<String, Model> models = swagger.getDefinitions();

        if (paths != null) {
            for (String pathname : paths.keySet()) {
                Path path = paths.get(pathname);


                for (Map.Entry<HttpMethod, Operation> entry : path.getOperationMap().entrySet()) {
                    HttpMethod httpMethod = entry.getKey();
                    Operation operation = entry.getValue();
                    List<Parameter> parameters = operation.getParameters();

                    if (parameters != null) {
                        for (Parameter parameter : parameters) {
                            if (parameter instanceof BodyParameter) {
                                BodyParameter bp = (BodyParameter) parameter;
                                if (bp.getSchema() != null) {
                                    Model model = bp.getSchema();
                                    if (model instanceof ModelImpl) {
                                        ModelImpl obj = (ModelImpl) model;
                                        if (obj.getType() == null || "object".equals(obj.getType())) {
                                            if (obj.getProperties() != null && obj.getProperties().size() > 0) {
                                                flattenProperties(obj.getProperties(), pathname);
                                                String modelName = resolveModelName(obj.getTitle(), toInlineName(pathname, httpMethod, operation, parameter, bp.getName()));
                                                bp.setSchema(new RefModel(modelName));
                                                addGenerated(modelName, model);
                                                swagger.addDefinition(modelName, model);
                                            }
                                        }
                                    } else if (model instanceof ArrayModel) {
                                        ArrayModel am = (ArrayModel) model;
                                        Property inner = am.getItems();

                                        if (inner instanceof ObjectProperty) {
                                            ObjectProperty op = (ObjectProperty) inner;
                                            if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                flattenProperties(op.getProperties(), pathname);
                                                String modelName = resolveModelName(op.getTitle(), toInlineName(pathname, httpMethod, operation, parameter, bp.getName()));
                                                Model innerModel = modelFromProperty(op, modelName);
                                                String existing = matchGenerated(innerModel);
                                                if (existing != null) {
                                                    am.setItems(new RefProperty(existing));
                                                } else {
                                                    am.setItems(new RefProperty(modelName));
                                                    addGenerated(modelName, innerModel);
                                                    swagger.addDefinition(modelName, innerModel);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Map<String, Response> responses = operation.getResponses();
                    if (responses != null) {
                        for (String key : responses.keySet()) {
                            Response response = responses.get(key);
                            if (response.getSchema() != null) {
                                Property property = response.getSchema();
                                if (property instanceof ObjectProperty) {
                                    ObjectProperty op = (ObjectProperty) property;
                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                        String modelName = resolveModelName(op.getTitle(), toInlineName(pathname, httpMethod, operation, null, key));
                                        Model model = modelFromProperty(op, modelName);
                                        String existing = matchGenerated(model);
                                        if (existing != null) {
                                            response.setSchema(this.makeRefProperty(existing, property));
                                        } else {
                                            response.setSchema(this.makeRefProperty(modelName, property));
                                            addGenerated(modelName, model);
                                            swagger.addDefinition(modelName, model);
                                        }
                                    }
                                } else if (property instanceof ArrayProperty) {
                                    ArrayProperty ap = (ArrayProperty) property;
                                    Property inner = ap.getItems();

                                    if (inner instanceof ObjectProperty) {
                                        ObjectProperty op = (ObjectProperty) inner;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            String modelName = resolveModelName(op.getTitle(), toInlineName(pathname, httpMethod, operation, null, key));
                                            Model innerModel = modelFromProperty(op, modelName);
                                            String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                ap.setItems(this.makeRefProperty(existing, op));
                                            } else {
                                                ap.setItems(this.makeRefProperty(modelName, op));
                                                addGenerated(modelName, innerModel);
                                                swagger.addDefinition(modelName, innerModel);
                                            }
                                        }
                                    }
                                } else if (property instanceof MapProperty) {
                                    MapProperty mp = (MapProperty) property;

                                    Property innerProperty = mp.getAdditionalProperties();
                                    if (innerProperty instanceof ObjectProperty) {
                                        ObjectProperty op = (ObjectProperty) innerProperty;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            String modelName = resolveModelName(op.getTitle(), toInlineName(pathname, httpMethod, operation, null, key));
                                            Model innerModel = modelFromProperty(op, modelName);
                                            String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                mp.setAdditionalProperties(new RefProperty(existing));
                                            } else {
                                                mp.setAdditionalProperties(new RefProperty(modelName));
                                                addGenerated(modelName, innerModel);
                                                swagger.addDefinition(modelName, innerModel);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // definitions
        if (models != null) {
            List<String> modelNames = new ArrayList<String>(models.keySet());
            for (String modelName : modelNames) {
                Model model = models.get(modelName);
                if (model instanceof ModelImpl) {
                    ModelImpl m = (ModelImpl) model;

                    Map<String, Property> properties = m.getProperties();
                    flattenProperties(properties, modelName);

                } else if (model instanceof ArrayModel) {
                    ArrayModel m = (ArrayModel) model;
                    Property inner = m.getItems();
                    if (inner instanceof ObjectProperty) {
                        ObjectProperty op = (ObjectProperty) inner;
                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                            String innerModelName = resolveModelName(op.getTitle(), modelName + "_inner");
                            Model innerModel = modelFromProperty(op, innerModelName);
                            String existing = matchGenerated(innerModel);
                            if (existing == null) {
                                swagger.addDefinition(innerModelName, innerModel);
                                addGenerated(innerModelName, innerModel);
                                m.setItems(new RefProperty(innerModelName));
                            } else {
                                m.setItems(new RefProperty(existing));
                            }
                        }
                    }
                } else if (model instanceof ComposedModel) {
                    ComposedModel m = (ComposedModel) model;
                    if (m.getChild() != null) {
                        Map<String, Property> properties = m.getChild().getProperties();
                        flattenProperties(properties, modelName);
                    }
                }
            }
        }
    }

    private String resolveModelName(String title, String key) {
        if (title == null) {
            return uniqueName(key);
        } else {
            return uniqueName(title);
        }
    }

    public String matchGenerated(Model model) {
        if (this.skipMatches) {
            return null;
        }
        String json = Json.pretty(model);
        if (generatedSignature.containsKey(json)) {
            return generatedSignature.get(json);
        }
        return null;
    }

    public void addGenerated(String name, Model model) {
        generatedSignature.put(Json.pretty(model), name);
    }

    public String uniqueName(String key) {
        int count = 0;
        boolean done = false;
        key = key.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
        // should not be
        // assigned. Also declare
        // the methods parameters
        // as 'final'.
        while (!done) {
            String name = key;
            if (count > 0) {
                name = key + "_" + count;
            }
            if (swagger.getDefinitions() == null) {
                return name;
            } else if (!swagger.getDefinitions().containsKey(name)) {
                return name;
            }
            count += 1;
        }
        return key;
    }

    public void flattenProperties(Map<String, Property> properties, String path) {
        if (properties == null) {
            return;
        }
        Map<String, Property> propsToUpdate = new HashMap<String, Property>();
        Map<String, Model> modelsToAdd = new HashMap<String, Model>();
        for (String key : properties.keySet()) {
            Property property = properties.get(key);
            if (property instanceof ObjectProperty && ((ObjectProperty) property).getProperties() != null
                    && ((ObjectProperty) property).getProperties().size() > 0) {

                ObjectProperty op = (ObjectProperty) property;

                String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                Model model = modelFromProperty(op, modelName);

                String existing = matchGenerated(model);

                if (existing != null) {
                    propsToUpdate.put(key, new RefProperty(existing));
                } else {
                    propsToUpdate.put(key, new RefProperty(modelName));
                    modelsToAdd.put(modelName, model);
                    addGenerated(modelName, model);
                    swagger.addDefinition(modelName, model);
                }
            } else if (property instanceof ArrayProperty) {
                ArrayProperty ap = (ArrayProperty) property;
                Property inner = ap.getItems();

                if (inner instanceof ObjectProperty) {
                    ObjectProperty op = (ObjectProperty) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Model innerModel = modelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            ap.setItems(new RefProperty(existing));
                        } else {
                            ap.setItems(new RefProperty(modelName));
                            addGenerated(modelName, innerModel);
                            swagger.addDefinition(modelName, innerModel);
                        }
                    }
                }
            } else if (property instanceof MapProperty) {
                MapProperty mp = (MapProperty) property;
                Property inner = mp.getAdditionalProperties();

                if (inner instanceof ObjectProperty) {
                    ObjectProperty op = (ObjectProperty) inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Model innerModel = modelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            mp.setAdditionalProperties(new RefProperty(existing));
                        } else {
                            mp.setAdditionalProperties(new RefProperty(modelName));
                            addGenerated(modelName, innerModel);
                            swagger.addDefinition(modelName, innerModel);
                        }
                    }
                }
            }
        }
        if (propsToUpdate.size() > 0) {
            for (String key : propsToUpdate.keySet()) {
                properties.put(key, propsToUpdate.get(key));
            }
        }
        for (String key : modelsToAdd.keySet()) {
            swagger.addDefinition(key, modelsToAdd.get(key));
            this.addedModels.put(key, modelsToAdd.get(key));
        }
    }

    @SuppressWarnings("static-method")
    public Model modelFromProperty(ArrayProperty object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        Property inner = object.getItems();
        if (inner instanceof ObjectProperty) {
            ArrayModel model = new ArrayModel();
            model.setDescription(description);
            model.setExample(example);
            model.setItems(object.getItems());
            return model;
        }

        return null;
    }

    public Model modelFromProperty(ObjectProperty object, String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        String name = object.getName();
        Xml xml = object.getXml();
        Map<String, Property> properties = object.getProperties();

        ModelImpl model = new ModelImpl();
        model.setDescription(description);
        model.setExample(example);
        model.setName(name);
        model.setXml(xml);

        if (properties != null) {
            flattenProperties(properties, path);
            model.setProperties(properties);
        }

        return model;
    }

    @SuppressWarnings("static-method")
    public Model modelFromProperty(MapProperty object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        ArrayModel model = new ArrayModel();
        model.setDescription(description);
        model.setExample(example);
        model.setItems(object.getAdditionalProperties());

        return model;
    }

    /**
     * Make a RefProperty
     */
    public Property makeRefProperty(String ref, Property property) {
        RefProperty newProperty = new RefProperty(ref);
        this.copyVendorExtensions(property, newProperty);
        return newProperty;
    }

    /**
     * Copy vendor extensions from Property to another Property
     */
    public void copyVendorExtensions(Property source, AbstractProperty target) {
        Map<String, Object> vendorExtensions = source.getVendorExtensions();
        for (String extName : vendorExtensions.keySet()) {
            target.setVendorExtension(extName, vendorExtensions.get(extName));
        }
    }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    public void setSkipMatches(boolean skipMatches) {
        this.skipMatches = skipMatches;
    }

}
