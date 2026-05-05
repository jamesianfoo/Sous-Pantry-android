package com.souspantry.app.ui.pantry;

import com.souspantry.app.data.repository.PantryRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class PantryViewModel_Factory implements Factory<PantryViewModel> {
  private final Provider<PantryRepository> repoProvider;

  public PantryViewModel_Factory(Provider<PantryRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public PantryViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static PantryViewModel_Factory create(Provider<PantryRepository> repoProvider) {
    return new PantryViewModel_Factory(repoProvider);
  }

  public static PantryViewModel newInstance(PantryRepository repo) {
    return new PantryViewModel(repo);
  }
}
