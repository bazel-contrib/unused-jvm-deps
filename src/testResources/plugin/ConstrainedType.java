package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ConstrainedType {

  public static void useNested(NestedClass input) {
    System.out.println(input.getMessage());
  }

  public static NestedClass getNested() {
    return new NestedClass();
  }

  public static class BaseNestedClass {
    protected String message = "Hello world!";
  }

  public static class NestedClass extends BaseNestedClass implements MyInterface {

    @Override
    public String getMessage() {
      return null;
    }
  }

  public static interface MyInterface {
    String getMessage();
  }
}
