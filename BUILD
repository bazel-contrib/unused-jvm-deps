java_plugin(
    name = "autovalue_plugin",
    generates_api = 1,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [
        "@maven//:com_google_auto_value_auto_value",
    ],
)

java_library(
    name = "autovalue",
    exported_plugins = [
        ":autovalue_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
)

java_library(
    name = "autoservice",
    exported_plugins = [
        ":autoservice_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "@maven//:com_google_auto_service_auto_service_annotations",
    ],
)

java_plugin(
    name = "autoservice_plugin",
    generates_api = 1,
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = [
        "@maven//:com_google_auto_service_auto_service",
    ],
)
