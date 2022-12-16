genrule(
    name = "generated-genrule",
    outs = ["generated.srcjar"],
    cmd = "$(location //src/main/codegen:DepAnalyzerJooqCodeGenerator) $@",
    tools = ["//src/main/codegen:DepAnalyzerJooqCodeGenerator"],
    visibility = ["//src/main:__subpackages__"],
)

java_library(
    name = "generated",
    srcs = [":generated.srcjar"],
    visibility = ["//src/main:__subpackages__"],
    deps = [
        "@maven//:org_jooq_jooq",
    ],
)
