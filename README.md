# unused-jvm-deps
[![CI](https://github.com/bazel-contrib/unused-jvm-deps/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/bazel-contrib/unused-jvm-deps/actions/workflows/ci.yaml)

Command-line tool for finding Bazel dependencies which are not actually used in code.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=U7rC5xHL8o4" target="_blank"><img src="http://img.youtube.com/vi/U7rC5xHL8o4/0.jpg" alt="Watch a quick talk about this tool from BazelCon 2022" width="600" height="400" border="10" /></a>

## What is an unused dependency?

An unused Bazel dependency is an unnecessary entry in the `deps` field of a `java_library` or `java_binary` target in
a `BUILD` file, meaning the code being compiled doesn't actually have any dependency on that library. Over time, unused 
dependencies accumulate as code changes and `BUILD` files are not updated to reflect
those changes.

## Why are unused dependencies bad?

Unused dependencies:

- increase local build and sync times
- increase CI run times
- lead to larger deployment artifacts

The problems posed by unused dependencies will only get worse as your code grows and ages... *unless* there were a way
to programmatically detect and remove them. Luckily, there now is!

## How do I remove unused dependencies from my code?

1. Run `unused-deps.sh <bazelPath> <outputFile>`
    1. `bazelPath`  is the path to the target or directory to analyze in Bazel label format. This can either mean "
       analyze" this target or analyze all targets underneath this directory.
    2. `outputFile`  is the absolute path to the file to which to write the Buildozer commands
    3. This could take a while (in our experience, running it on four million lines of Java will take around six hours).
       If you are analyzing a smaller subsection it should be much faster. The script will print out the progress of the
       build that it runs, so you can get a rough idea of how much time is left.
2. Run the Buildozer commands with `bash <outputFile>`  to programmatically remove the detected unused dependencies
3. Verify that the target still compiles using `bazel build <bazelPath>` . If your build fails, read the **
   Troubleshooting** section below.

```
> ./unused-deps.sh //src/main/java/com/stripe/payments/server/worker/... worker-commands.txt
...
Ignored dependencies: 3802
------------------------------
Considered dependencies: 467
Used dependencies: 432 (93%)
Unused dependencies: 35 (7%)
------------------------------
Time elapsed: 0 seconds
Wrote buildozer commands to remove unused dependencies to: worker-commands.txt
 
 
> bash worker-commands.txt
fixed src/main/java/com/stripe/payments/server/worker/db/BUILD
...
fixed src/main/java/com/stripe/payments/server/worker/workflows/interfaces/BUILD
 
 
> bazel build //src/main/java/com/stripe/payments/server/worker/db/...
...
INFO: Build completed successfully, 2867 total actions
```

## Limitations

This tool does not support the following:

- Third-party dependencies (https://github.com/bazel-contrib/unused-jvm-deps/issues/2)
- Scala (https://github.com/bazel-contrib/unused-jvm-deps/issues/3) and Kotlin (https://github.com/bazel-contrib/unused-jvm-deps/issues/4)
- Java test targets (https://github.com/bazel-contrib/unused-jvm-deps/issues/5)
