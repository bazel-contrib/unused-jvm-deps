package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ImportsIgnoreDefinedInFile {

  public int method() {
    ClassDefinedInThisFile object = new ClassDefinedInThisFile();
    return object.field + object.method();
  }

  private class ClassDefinedInThisFile {
    private int field = 0;

    private int method() {
      return 0;
    }
  }
}
