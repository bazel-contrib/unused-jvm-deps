package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ImportsClassExtension extends Superclass<Number> implements SimpleInterface {

  @Override
  public String getMessage() {
    return superField + superStaticMethod();
  }
}
