package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ImportsMemberSelects {
  public int method() {
    return ExportsStatics.NestedStaticClass.nestedMethod()
        .length(); // NestedStaticClass, nestedMethod and its return type (String) should be listed
  }
}
