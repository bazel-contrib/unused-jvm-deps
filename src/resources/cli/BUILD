filegroup(
    name = "cli",
    srcs = [
        "BUILD.override",
        "WORKSPACE.override",
        "defs.bzl.override",
    ],
    visibility = ["//visibility:public"],
)

# This is a hack to get defs.bzl.override to work as a bzl file in our own codebase *and* allow it
# to be injected as an overridden repository when running the actual CLI tool
java_plugin(
    name = "analyzer",
    visibility = ["//visibility:public"],
    deps = ["//src/main/plugin:plugin-binary_deploy.jar"],
)
