package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class ImportsConstraints<T extends Duration> {

  List<? extends Clock> wildCardConstraint;
}
