package com.souspantry.app.ui.shopping;

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
public final class ShoppingViewModel_Factory implements Factory<ShoppingViewModel> {
  private final Provider<ApiService> apiProvider;

  private final Provider<PantryRepository> repoProvider;

  public ShoppingViewModel_Factory(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    this.apiProvider = apiProvider;
    this.repoProvider = repoProvider;
  }

  @Override
  public ShoppingViewModel get() {
    return newInstance(apiProvider.get(), repoProvider.get());
  }

  public static ShoppingViewModel_Factory create(Provider<ApiService> apiProvider,
      Provider<PantryRepository> repoProvider) {
    return new ShoppingViewModel_Factory(apiProvider, repoProvider);
  }

  public static ShoppingViewModel newInstance(ApiService api, PantryRepository repo) {
    return new ShoppingViewModel(api, repo);
  }
}
