java_library(
    name = "bazel",
    srcs = glob(["*.java"]),
    visibility = [
        "//src/main:__subpackages__",
        "//src/test:__subpackages__",
    ],
    deps = [
        "//:autovalue",
        "@bazel_tools//src/main/protobuf:build_java_proto",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_commons_commons_lang3",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

java_binary(
    name = "CostGraphMain",
    srcs = ["CostGraphMain.java"],
    main_class = "com.stripe.build.dependencygraph.CostGraphMain",
    visibility = ["//visibility:public"],
    deps = [":bazel"],
)
