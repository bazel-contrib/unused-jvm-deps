package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

import java.beans.JavaBean;
import javax.annotation.Nullable;
import javax.validation.constraints.Positive;

@JavaBean
public class ImportsAnnotations {

  @Positive public int annotatedField = 1;

  @Nullable
  public String annotatedMethod() {
    return null;
  }
}
