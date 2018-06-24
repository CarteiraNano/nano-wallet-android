package com.carteiranano.app.di.application;


import javax.inject.Named;

import com.carteiranano.app.analytics.AnalyticsService;
import com.carteiranano.app.di.analytics.AnalyticsModule;
import com.carteiranano.app.di.persistence.PersistenceModule;
import com.carteiranano.app.util.SharedPreferencesUtil;
import dagger.Component;
import io.realm.Realm;

@Component(modules = {ApplicationModule.class, PersistenceModule.class, AnalyticsModule.class})
@ApplicationScope
public interface ApplicationComponent {
    // persistence module
    SharedPreferencesUtil provideSharedPreferencesUtil();

    // database
    Realm provideRealm();

    AnalyticsService provideAnalyticsService();

    // encryption key
    @Named("encryption_key")
    byte[] providesEncryptionKey();
}
