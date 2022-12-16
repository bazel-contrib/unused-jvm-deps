java_binary(
    name = "cli",
    srcs = glob(["*.java"]),
    main_class = "com.stripe.build.dependencyanalyzer.cli.Main",
    resources = [
        "//src/main/plugin:plugin-binary_deploy.jar",
        "//src/resources/cli",
    ],
    visibility = ["//visibility:private"],
    deps = [
        "//src/main/analysis",
        "//src/main/bazel",
        "//src/main/collection",
        "//src/main/database",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:info_picocli_picocli",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)
