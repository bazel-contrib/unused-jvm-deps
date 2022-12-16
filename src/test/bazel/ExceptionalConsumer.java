package com.stripe.build.dependencyanalyzer.bazel;

@FunctionalInterface
interface ExceptionalConsumer<A> {
  void apply(A a) throws Exception;
}
