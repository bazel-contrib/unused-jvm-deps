package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

import static uppsala.src.test.resources.com.stripe.build.dependencyanalyzer.ExportsStatics.CONSTANT;

public class ImportsStatics {
  public int method() {
    return ExportsStatics.staticField + ExportsStatics.staticMethod();
  }

  public int methodWithStaticImport() {
    return CONSTANT;
  }
}
