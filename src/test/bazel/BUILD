[java_test(
    name = "bazel_" + s,
    size = "small",
    srcs = [s],
    test_class = "com.stripe.build.dependencyanalyzer.bazel." + s.replace(".java", ""),
    deps = [
        ":utils",
        "//src/main/bazel",
        "@bazel_tools//src/main/protobuf:build_java_proto",
        "@maven//:com_google_truth_truth",
    ],
) for s in glob(["*Test.java"])]

java_library(
    name = "utils",
    srcs = ["ExceptionalConsumer.java"],
)
