java_library(
    name = "sql-runner",
    srcs = ["SqlScriptRunner.java"],
    visibility = ["//src/main:__subpackages__"],
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_xerial_sqlite_jdbc",
    ],
)

java_binary(
    name = "DepAnalyzerJooqCodeGenerator",
    srcs = ["JooqCodeGenerator.java"],
    main_class = "com.stripe.build.dependencyanalyzer.codegen.JooqCodeGenerator",
    resources = [
        "//src/resources/database",
    ],
    visibility = ["//src/main:__subpackages__"],
    deps = [
        ":sql-runner",
        "@maven//:com_google_guava_guava",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_jooq_jooq",
        "@maven//:org_jooq_jooq_codegen",
        "@maven//:org_jooq_jooq_meta",
        "@maven//:org_xerial_sqlite_jdbc",
    ],
)
