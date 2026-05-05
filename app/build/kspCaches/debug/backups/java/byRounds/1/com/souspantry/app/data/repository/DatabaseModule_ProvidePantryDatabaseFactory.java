package com.souspantry.app.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvidePantryDatabaseFactory implements Factory<PantryDatabase> {
  private final Provider<Context> ctxProvider;

  public DatabaseModule_ProvidePantryDatabaseFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public PantryDatabase get() {
    return providePantryDatabase(ctxProvider.get());
  }

  public static DatabaseModule_ProvidePantryDatabaseFactory create(Provider<Context> ctxProvider) {
    return new DatabaseModule_ProvidePantryDatabaseFactory(ctxProvider);
  }

  public static PantryDatabase providePantryDatabase(Context ctx) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePantryDatabase(ctx));
  }
}
