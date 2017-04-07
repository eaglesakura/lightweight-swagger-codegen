# Light Weight Swagger codegen

`swagger.yaml` もしくは `swagger.json` からクライアント / サーバー用コードを生成します。

# example.

```groovy
@GrabResolver(name = "eaglesakura", root = "https://dl.bintray.com/eaglesakura/maven/")
@Grab("com.eaglesakura:lightweight-swagger-codegen:35")
import java.lang.Object;

static main(String[] args) {
    // Generate swagger.json
    com.eaglesakura.swagger.generator.Generator.main([
            "generate",
            "-l", "io.swagger.codegen.languages.SwaggerGenerator",
            "-o", "path/to/output/dir",
            "-i", "path/to/input/swagger.yaml",
    ] as String[])
    
    // Generate Android client binding
    com.eaglesakura.swagger.generator.Generator.main([
            "generate",
            "-l", "com.eaglesakura.swagger.generator.AndroidCustomClientCodegen",
            "-o", "path/to/output/dir",
            "-i", "path/to/input/swagger.json",
            "-c", "path/to/configure/config.json"
    ] as String[])

    // Generate GAE/Go server binding
    com.eaglesakura.swagger.generator.Generator.main([
            "generate",
            "-l", "com.eaglesakura.swagger.generator.GaeGoCodegen",
            "-o", "path/to/output/dir",
            "-i", "path/to/input/swagger.json",
            "-c", "path/to/configure/config.json"
    ] as String[])

    // GO formatting
    println(["go", "fmt", "./path/to/output/dir/..."].execute().text)
}
```

## LICENSE

```
Copyright 2017 @eaglesakura / 2016 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
