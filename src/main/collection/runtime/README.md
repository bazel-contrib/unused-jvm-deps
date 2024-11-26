# Runtime dependency analysis

This is a Java agent which instruments an actual run of a Java program (e.g., a JUnit test) to 
determine which Java classes were actually needed to run the program.

## Usage

In this repo:
```sh
bazel build //src/main/collection/runtime:collect_runtime_symbols_deploy.jar
```

In the target repo:

```sh
export JAR_PATH=$HOME/unused-jvm-deps/bazel-bin/src/main/collection/runtime/collect_runtime_symbols_deploy.jar
export RESULT_PATH=$(pwd)/runtime-output.json
bazel test //src/test/my/java:test \
    --spawn_strategy=standalone \
    --test_arg=--wrapper_script_flag=--jvm_flag=-javaagent:$JAR_PATH=$RESULT_PATH
```

During development debugging of this feature, you probably also want to add `--cache_test_results=no --test_output=streamed`.