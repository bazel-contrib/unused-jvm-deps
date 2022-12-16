package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ExportsStatics {
  public static final int CONSTANT = 0;

  public static int staticField = 0;

  public static int staticMethod() {
    return 0;
  }

  public static class NestedStaticClass {
    public static int nestedField = 0;

    public static String nestedMethod() {
      return "Hello";
    }
  }
}
