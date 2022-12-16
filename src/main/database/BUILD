java_library(
    name = "database",
    srcs = glob(["*.java"]),
    resources = [
        "//src/resources/database",
    ],
    visibility = ["//src/main:__subpackages__"],
    runtime_deps = [
        "@maven//:org_xerial_sqlite_jdbc",
    ],
    deps = [
        "//src/main/codegen:sql-runner",
        "//src/main/database/generated",
        "@maven//:com_google_guava_guava",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:org_jooq_jooq",
    ],
)
