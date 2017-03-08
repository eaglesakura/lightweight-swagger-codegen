#! groovy

// example.

// swagger.json
//      $ ./swagger-codegen.groovy generate -l io.swagger.codegen.languages.SwaggerGenerator -o ./output -i swagger.yaml

// Android client.
//      $ ./swagger-codegen.groovy generate -l com.eaglesakura.swagger.generator.AndroidCustomClientCodegen -o path/to/output -i path/to/swagger.json -c path/to/config.json

// GAE/Go Server.
//      $ ./swagger-codegen.groovy generate -l com.eaglesakura.swagger.generator.GaeGoCodegen -o path/to/output -i path/to/swagger.json -c path/to/config.json

@GrabResolver(name = "eaglesakura", root = "http://eaglesakura.github.io/maven/")
@Grab("com.eaglesakura:swagger-codegen-lw:1.0.24")
import java.lang.Object;

static main(String[] args) {
    com.eaglesakura.swagger.generator.Generator.main(args)
}
