package uppsala.src.test.resources.com.stripe.build.dependencyanalyzer;

public class ExportsNestedPrivateClass {

  private class NestedPrivateClass {

    // a public class nested within a private nested class should not be an exported symbol
    public class PublicClassWithinPrivateClass {
      public int nestedMember;
    }
  }
}
