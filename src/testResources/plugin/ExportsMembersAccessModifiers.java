package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ExportsMembersAccessModifiers {
  private int privateField = 0; // symbol should not be exported

  private int privateMethod() { // symbol should not be exported
    return 0;
  }

  int packagePrivateField = 0;

  int packagePrivateMethod() {
    return 0;
  }

  protected int protectedField = 0;

  protected int protectedMethod() {
    return 0;
  }

  public int publicField = 0;

  public int publicMethod() {
    return 0;
  }
}
