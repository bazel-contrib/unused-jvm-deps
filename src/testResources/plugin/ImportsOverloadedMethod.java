package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ImportsOverloadedMethod {
  public void invoke() {
    ExportsOverloadedMethod.overloaded(new ExportsOverloadedMethod.TypeA());
  }
}
