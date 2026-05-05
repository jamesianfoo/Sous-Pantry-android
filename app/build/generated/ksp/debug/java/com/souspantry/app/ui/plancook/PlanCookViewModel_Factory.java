package com.souspantry.app.ui.plancook;

import com.souspantry.app.data.repository.PantryRepository;
import com.souspantry.app.services.ApiService;
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
public final class PlanCookViewModel_Factory implements Factory<PlanCookViewModel> {
  private final Provider<ApiService> apiProvider;

  private final Provider<PantryRepository> repoProvider;

  public PlanCookViewModel_Factory(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    this.apiProvider = apiProvider;
    this.repoProvider = repoProvider;
  }

  @Override
  public PlanCookViewModel get() {
    return newInstance(apiProvider.get(), repoProvider.get());
  }

  public static PlanCookViewModel_Factory create(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    return new PlanCookViewModel_Factory(apiProvider, repoProvider);
  }

  public static PlanCookViewModel newInstance(ApiService api, PantryRepository repo) {
    return new PlanCookViewModel(api, repo);
  }
}
