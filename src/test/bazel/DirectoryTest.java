package com.stripe.build.dependencyanalyzer.bazel;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DirectoryTest {
  @Test
  public void canParseDirectoryFromAbsolutePathWithRowAndCol() {
    var ex =
        "/Users/me/stripe/myrepo/uppsala/src/test/java/com/stripe/terminal/terminal/server/cerebro/actions/scan/BUILD:4:35";
    var actual = Directory.parse(ex, "/Users/me/stripe/myrepo").get();

    assertThat(actual.value())
        .isEqualTo(
            "uppsala/src/test/java/com/stripe/terminal/terminal/server/cerebro/actions/scan");
  }

  @Test
  public void canParseDirectoryFromLabel() {
    var ex = "/src/uppsala/src/main/proto/com/stripe/cards_auth/server/message/visa/BUILD:545:19";
    var res = Directory.parse(ex, "/src").get();

    assertThat(res.value())
        .isEqualTo("uppsala/src/main/proto/com/stripe/cards_auth/server/message/visa");
  }

  @Test
  public void canParseDirectoryFromTargetLabel() {
    var ex = "/src/uppsala/src/main/proto/com/stripe/cards_auth/server/message/visa/BUILD:545:19";
    var res = Directory.parse(ex, "/src").get();

    assertThat(res.value())
        .isEqualTo("uppsala/src/main/proto/com/stripe/cards_auth/server/message/visa");
  }

  @Test
  public void canParseFailingExampleFromProd2() {
    var ex = "//.ijwb/.run/efi:Run efi.run.xml";
    var label = BazelFileLabel.of(ex);

    assertThat(label.isPresent()).isTrue();

    var dir = label.get().toDirectory();
    assertThat(dir.value()).isEqualTo(".ijwb/.run/efi");
  }

  @Test
  public void canParseDirectoryFromBazelFileLabel() {
    var rootFileLabels = Set.of("//:mergeq.txt", "//:.bazelrc");

    rootFileLabels.forEach(
        rawFileLabel -> {
          var fileLabel = BazelFileLabel.of(rawFileLabel);
          assertThat(fileLabel.isPresent()).isTrue();

          var expected = "/";
          var actual = fileLabel.get().toDirectory().value();
          assertThat(actual).isEqualTo(expected);
        });
  }
}
