#!/usr/bin/env bash

set -euo pipefail

# <--readme-->
# Analyzes the Bazel and Java source dependency graphs, detects unused Bazel dependencies, and outputs
# Buildozer commands to remove the unused dependencies from BUILD files.
# <--readme-->

if [[ $# -ne 2 ]] ; then
    echo "usage: $0 <bazel_package> <output_file>"
    echo 'Parameters'
    echo '----------'
    echo 'bazel_package: path from root of Bazel workspace prefixed with "//", ex: //src/main/java/com/stripe/payments/...'
    echo 'output_file: absolute path to output file, ex: output.txt'
    exit 1
fi

BAZEL_WORKSPACE=$(pwd)
cd "$(dirname "${BASH_SOURCE[0]}")"

BAZEL_TARGET_PATTERN=$1
OUTPUT_FILE=$(pwd)/$2

UNUSED_DEPS_DATABASES=~/.unused-deps-cache
mkdir -p "$UNUSED_DEPS_DATABASES"

BAZEL_TARGET_PATTERN_WITHOUT_DOTS=${BAZEL_TARGET_PATTERN%/...} # drop trailing /...
DB_NAME=${BAZEL_TARGET_PATTERN_WITHOUT_DOTS:2} # drop leading //
DB_NAME=${DB_NAME//\//-}.db
DB_PATH=$UNUSED_DEPS_DATABASES/$DB_NAME
rm -rf "$DB_PATH" # remove old database if exists

# collect dependency graph database
bazel run //src/main/cli -- \
    collect \
    "$BAZEL_TARGET_PATTERN" \
    "$BAZEL_WORKSPACE" \
    "$DB_PATH" \
    --ignore_cache \
    --debug

# analyze dependency graph database and output commands to remove unused deps
bazel run //src/main/cli -- \
    analyze \
    unused \
    "$DB_PATH" \
    "$BAZEL_WORKSPACE" \
    --filter="$BAZEL_TARGET_PATTERN_WITHOUT_DOTS" \
    --output="$OUTPUT_FILE"

