package com.carteiranano.app.di.analytics;

import android.content.Context;

import com.carteiranano.app.analytics.AnalyticsService;
import com.carteiranano.app.di.application.ApplicationScope;
import com.carteiranano.app.di.persistence.PersistenceModule;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;

@Module(includes = PersistenceModule.class)
public class AnalyticsModule {
    @Provides
    @ApplicationScope
    AnalyticsService providesAnalyticsService(Context context, Realm realm) {
        return new AnalyticsService(context.getApplicationContext(), realm);
    }
}
