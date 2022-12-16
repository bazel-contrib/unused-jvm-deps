package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ExportsOverloadedMethod {
  public static void overloaded(TypeA arg) {}

  public static void overloaded(TypeB arg) {}

  public static class TypeA {}

  public static class TypeB {}
}
