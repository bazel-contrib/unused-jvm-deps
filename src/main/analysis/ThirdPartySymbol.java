package com.stripe.build.dependencyanalyzer.analysis;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ThirdPartySymbol {
  public abstract String getFullyQualifiedName();

  public abstract String getTarget();

  public abstract String getLanguage();

  public abstract String getLanguageVisibility();

  public abstract String getBazelVisibility();

  public static ThirdPartySymbol create(
      String fullyQualifiedName,
      String target,
      String language,
      String languageVisilibity,
      String bazelVisibility) {
    return new AutoValue_ThirdPartySymbol(
        fullyQualifiedName, target, language, languageVisilibity, bazelVisibility);
  }
}
