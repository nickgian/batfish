package(
    default_testonly = True,
    default_visibility = ["//visibility:public"],
)

load("@batfish//skylark:junit.bzl", "junit_tests")

java_library(
    name = "testlib",
    srcs = [
        "FlatJuniperRecoveryExtractor.java",
    ],
    deps = [
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/flatjuniper",
        "@antlr4_runtime//:compile",
    ],
)

junit_tests(
    name = "tests",
    srcs = glob([
        "**/*Test.java",
    ]),
    resources = [
        "//projects/batfish/src/test/resources/org/batfish/grammar/juniper/testconfigs",
        "//projects/batfish/src/test/resources/org/batfish/grammar/juniper/testrigs",
    ],
    deps = [
        ":testlib",
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "//projects/batfish-common-protocol:common_testlib",
        "//projects/batfish/src/main/antlr4/org/batfish/grammar/flatjuniper",
        "@antlr4_runtime//:compile",
        "@guava//:compile",
        "@hamcrest//:compile",
    ],
)
