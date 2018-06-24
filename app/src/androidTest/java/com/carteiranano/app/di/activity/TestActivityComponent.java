package com.carteiranano.app.di.activity;

import com.carteiranano.app.di.application.ApplicationComponent;
import com.carteiranano.app.model.NanoWalletTest;
import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface TestActivityComponent extends ActivityComponent {
    void inject(NanoWalletTest nanoWalletTest);
}
