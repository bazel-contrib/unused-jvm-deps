java_library(
    name = "analysis",
    srcs = glob(["*.java"]),
    visibility = [
        "//src/main:__subpackages__",
        "//src/test:__subpackages__",
    ],
    deps = [
        "//:autovalue",
        "//src/main/database",
        "//src/main/database/generated",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_commons_commons_csv",
        "@maven//:org_apache_commons_commons_lang3",
    ],
)
