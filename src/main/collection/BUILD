java_library(
    name = "collection",
    srcs = glob(["*.java"]),
    visibility = [
        "//src/main:__subpackages__",
    ],
    deps = [
        "//:autovalue",
        "//src/main/bazel",
        "//src/main/database",
        "//src/main/database/generated",
        "//src/main/plugin:plugin-library",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_io_commons_io",
    ],
)
