package com.carteiranano.app.di.activity;

import com.google.gson.Gson;

import com.carteiranano.app.MainActivity;
import com.carteiranano.app.di.application.ApplicationComponent;
import com.carteiranano.app.model.NanoWallet;
import com.carteiranano.app.network.AccountService;
import com.carteiranano.app.ui.home.HomeFragment;
import com.carteiranano.app.ui.intro.IntroLegalFragment;
import com.carteiranano.app.ui.intro.IntroNewWalletFragment;
import com.carteiranano.app.ui.intro.IntroSeedFragment;
import com.carteiranano.app.ui.intro.IntroWelcomeFragment;
import com.carteiranano.app.ui.pin.CreatePinDialogFragment;
import com.carteiranano.app.ui.pin.PinDialogFragment;
import com.carteiranano.app.ui.receive.ReceiveDialogFragment;
import com.carteiranano.app.ui.send.SendFragment;
import com.carteiranano.app.ui.settings.SettingsDialogFragment;
import dagger.Component;

@Component(modules = {ActivityModule.class}, dependencies = {ApplicationComponent.class})
@ActivityScope
public interface ActivityComponent {
    @ActivityScope
    AccountService provideAccountService();

    // wallet
    NanoWallet provideNanoWallet();

    @ActivityScope
    Gson provideGson();

    void inject(AccountService accountService);

    void inject(CreatePinDialogFragment createPinDialogFragment);

    void inject(HomeFragment homeFragment);

    void inject(IntroLegalFragment introLegalFragment);

    void inject(IntroNewWalletFragment introNewWalletFragment);

    void inject(IntroWelcomeFragment introWelcomeFragment);

    void inject(IntroSeedFragment introSeedFragment);

    void inject(MainActivity mainActivity);

    void inject(NanoWallet nanoWallet);

    void inject(PinDialogFragment pinDialogFragment);

    void inject(ReceiveDialogFragment receiveDialogFragment);

    void inject(SendFragment sendFragment);

    void inject(SettingsDialogFragment settingsDialogFragment);
}
