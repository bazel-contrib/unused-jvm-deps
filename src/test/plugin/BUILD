[java_test(
    name = "plugin_" + s,
    size = "small",
    srcs = [s],
    resources = [
        "//src/testResources/plugin:test_classes",
    ],
    test_class = "com.stripe.build.dependencyanalyzer.plugin." + s.replace(".java", ""),
    deps = [
        ":plugin-test-library",
        "//src/main/plugin:plugin-library",
        "@maven//:com_google_testing_compile_compile_testing",
        "@maven//:org_apache_commons_commons_lang3",
    ],
) for s in glob(["*Test.java"])]

java_library(
    name = "plugin-test-library",
    srcs = glob(
        ["*.java"],
        exclude = ["*Test.java"],
    ),
    visibility = ["//visibility:private"],
    deps = [
        "//src/main/plugin:plugin-library",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_errorprone_javac",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)
