java_library(
    name = "plugin-library",
    srcs = glob(["*.java"]),
    #    visibility = [
    #        "//src/main:__subpackages__",
    #        "//src/test/plugin:__subpackages__",
    #    ],
    visibility = ["//visibility:public"],
    deps = [
        "//:autoservice",
        "//:autovalue",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_errorprone_error_prone_check_api",
        "@maven//:com_google_errorprone_javac",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)

java_binary(
    name = "plugin-binary",
    visibility = ["//visibility:public"],
    runtime_deps = [":plugin-library"],
)

java_plugin(
    name = "plugin",
    visibility = ["//visibility:public"],
    deps = [":plugin-library"],
)
