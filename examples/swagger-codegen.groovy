#! groovy

@GrabResolver(name = "eaglesakura", root = "http://eaglesakura.github.io/maven/")
@Grab("com.eaglesakura:swagger-codegen-lw:1.0.+")
import java.lang.Object;

static main(String[] args) {
    com.eaglesakura.swagger.generator.Generator.main(args)
}
