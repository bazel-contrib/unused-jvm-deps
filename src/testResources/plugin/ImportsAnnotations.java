package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

import java.beans.JavaBean;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@JavaBean
public class ImportsAnnotations {

  @Generated(value="com.example.MyGenerator")
  public int annotatedField = 1;

  @Nullable
  public String annotatedMethod() {
    return null;
  }
}
