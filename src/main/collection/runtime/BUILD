java_binary(
    name = "collect_runtime_symbols",
    srcs = glob(["**/*.java"]),
    deploy_manifest_lines = [
        "Premain-Class: com.stripe.build.dependencyanalyzer.collection.runtime.RuntimeSymbolCollectorAgent",
    ],
    deps = [
        "//:autovalue",
        "@maven//:com_google_code_gson_gson",
    ],
)
