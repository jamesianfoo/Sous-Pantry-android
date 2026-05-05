package com.souspantry.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PantryRepository_Factory implements Factory<PantryRepository> {
  private final Provider<PantryDao> daoProvider;

  public PantryRepository_Factory(Provider<PantryDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public PantryRepository get() {
    return newInstance(daoProvider.get());
  }

  public static PantryRepository_Factory create(Provider<PantryDao> daoProvider) {
    return new PantryRepository_Factory(daoProvider);
  }

  public static PantryRepository newInstance(PantryDao dao) {
    return new PantryRepository(dao);
  }
}
