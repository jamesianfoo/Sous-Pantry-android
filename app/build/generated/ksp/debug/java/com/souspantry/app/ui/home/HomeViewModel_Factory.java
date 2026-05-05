package com.souspantry.app.ui.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ApiService> apiProvider;

  private final Provider<PantryRepository> repoProvider;

  public HomeViewModel_Factory(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    this.apiProvider = apiProvider;
    this.repoProvider = repoProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(apiProvider.get(), repoProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    return new HomeViewModel_Factory(apiProvider, repoProvider);
  }

  public static HomeViewModel newInstance(ApiService api, PantryRepository repo) {
    return new HomeViewModel(api, repo);
  }
}
