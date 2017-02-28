#! groovy

// example.
// ./swagger-codegen.groovy generate -l com.eaglesakura.swagger.generator.AndroidCustomClientCodegen -o ./output -i swagger.json

@GrabResolver(name = "eaglesakura", root = "http://eaglesakura.github.io/maven/")
@Grab("com.eaglesakura:swagger-codegen-lw:1.0.12")
import java.lang.Object;

static main(String[] args) {
    com.eaglesakura.swagger.generator.Generator.main(args)
}
