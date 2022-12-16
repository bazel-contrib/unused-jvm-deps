package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

import java.io.Serializable;

public class Superclass<T extends Serializable> {
  public static String superStaticMethod() {
    return "super static";
  }

  protected String superField;

  public Superclass() {
    this.superField = "super";
  }
}
