package com.carteiranano.app.ui.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.ajalt.reprint.core.AuthenticationFailureReason;
import com.github.ajalt.reprint.core.Reprint;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import com.carteiranano.app.BuildConfig;
import com.carteiranano.app.R;
import com.carteiranano.app.analytics.AnalyticsEvents;
import com.carteiranano.app.analytics.AnalyticsService;
import com.carteiranano.app.bus.CreatePin;
import com.carteiranano.app.bus.Logout;
import com.carteiranano.app.bus.PinComplete;
import com.carteiranano.app.bus.RxBus;
import com.carteiranano.app.databinding.FragmentSettingsBinding;
import com.carteiranano.app.model.AvailableCurrency;
import com.carteiranano.app.model.Credentials;
import com.carteiranano.app.model.StringWithTag;
import com.carteiranano.app.network.AccountService;
import com.carteiranano.app.ui.common.ActivityWithComponent;
import com.carteiranano.app.ui.common.BaseDialogFragment;
import com.carteiranano.app.ui.common.KeyboardUtil;
import com.carteiranano.app.ui.common.WindowControl;
import com.carteiranano.app.ui.common.UIUtil;
import com.carteiranano.app.util.SharedPreferencesUtil;
import io.realm.Realm;

/**
 * Settings main screen
 */
public class SettingsDialogFragment extends BaseDialogFragment {
    private FragmentSettingsBinding binding;
    public static String TAG = SettingsDialogFragment.class.getSimpleName();
    private Boolean showCurrency = false;
    private AlertDialog fingerprintDialog;
    private Boolean clickedNewSeed = false;

    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;

    @Inject
    Realm realm;

    @Inject
    AccountService accountService;

    @Inject
    AnalyticsService analyticsService;

    @BindingAdapter("android:layout_marginTop")
    public static void setTopMargin(View view, float topMargin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMargins(layoutParams.leftMargin, Math.round(topMargin),
                layoutParams.rightMargin, layoutParams.bottomMargin);
        view.setLayoutParams(layoutParams);
    }

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of SettingsDialogFragment
     */
    public static SettingsDialogFragment newInstance() {
        Bundle args = new Bundle();
        SettingsDialogFragment fragment = new SettingsDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Modal_Window);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inject
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        analyticsService.track(AnalyticsEvents.SETTINGS_VIEWED);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_settings, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());
        binding.setShowCurrency(showCurrency);
        binding.setVersion(getString(R.string.version_display, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        setStatusBarWhite(view);

        // set disclaimer links
        createLink(binding.settingsDisclaimer, R.string.settings_disclaimer);

        // subscribe to bus
        RxBus.get().register(this);

        // set up spinner
        List<StringWithTag> availableCurrencies = getAllCurrencies();
        ArrayAdapter<StringWithTag> spinnerArrayAdapter = new ArrayAdapter<>(getContext(),
                R.layout.view_textview_spinner,
                availableCurrencies
        );

        spinnerArrayAdapter.setDropDownViewResource(R.layout.view_textview_spinner);
        binding.settingsLocalCurrencySpinner.setAdapter(spinnerArrayAdapter);
        binding.settingsLocalCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // save local currency to shared preferences
                StringWithTag swt = (StringWithTag) adapterView.getItemAtPosition(i);
                AvailableCurrency key = (AvailableCurrency) swt.tag;
                if (key != null) {
                    sharedPreferencesUtil.setLocalCurrency(key);
                    final HashMap<String, String> customData = new HashMap<>();
                    customData.put("currency", key.toString());
                    analyticsService.track(AnalyticsEvents.LOCAL_CURRENCY_SELECTED, customData);
                    // update currency amounts
                    accountService.requestSubscribe();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // set selected item with value saved in shared preferences
        binding.settingsLocalCurrencySpinner.setSelection(getIndexOf(sharedPreferencesUtil.getLocalCurrency(), availableCurrencies));

        // set the listener for Navigation
        Toolbar toolbar = view.findViewById(R.id.dialog_appbar);
        if (toolbar != null) {
            final SettingsDialogFragment window = this;
            TextView title = view.findViewById(R.id.dialog_toolbar_title);
            title.setText(R.string.settings_title);
            toolbar.setNavigationOnClickListener(v1 -> window.dismiss());
        }

        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            binding.settingsShowNewSeed.setVisibility(credentials.getNewlyGeneratedSeed() != null ? View.VISIBLE : View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    /**
     * Pin entered correctly
     *
     * @param pinComplete PinComplete object
     */
    @Subscribe
    public void receivePinComplete(PinComplete pinComplete) {
        showCopySeedAlert();
    }

    @Subscribe
    public void receiveCreatePin(CreatePin pinComplete) {
        realm.beginTransaction();
        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            credentials.setPin(pinComplete.getPin());
        }
        realm.commitTransaction();
        showCopySeedAlert();
    }

    /**
     * Get list of all of the available currencies
     *
     * @return Lost of all currencies the app supports
     */
    private List<StringWithTag> getAllCurrencies() {
        List<StringWithTag> itemList = new ArrayList<>();
        for (AvailableCurrency currency : AvailableCurrency.values()) {
            itemList.add(new StringWithTag(currency.getFullDisplayName(), currency));
        }
        return itemList;
    }

    /**
     * Get Index of a particular currency
     *
     * @return Index of a particular currency in the spinner
     */
    private int getIndexOf(AvailableCurrency currency, List<StringWithTag> availableCurrencies) {
        int i = 0;
        for (StringWithTag availableCurrency : availableCurrencies) {
            if (availableCurrency.tag.equals(currency)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    private void showSeed() {
        Credentials credentials = realm.where(Credentials.class).findFirst();

        if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered()) {
            // show fingerprint dialog
            LayoutInflater factory = LayoutInflater.from(getContext());
            @SuppressLint("InflateParams") final View viewFingerprint = factory.inflate(R.layout.view_fingerprint, null);
            showFingerprintDialog(viewFingerprint);
            com.github.ajalt.reprint.rxjava2.RxReprint.authenticate()
                    .subscribe(result -> {
                        switch (result.status) {
                            case SUCCESS:
                                showFingerprintSuccess(viewFingerprint);
                                break;
                            case NONFATAL_FAILURE:
                                showFingerprintError(result.failureReason, result.errorMessage, viewFingerprint);
                                break;
                            case FATAL_FAILURE:
                                showFingerprintError(result.failureReason, result.errorMessage, viewFingerprint);
                                break;
                        }
                    });
        } else if (credentials != null && credentials.getPin() != null) {
            showPinScreen(getString(R.string.settings_fingerprint_description));
        } else if (credentials != null && credentials.getPin() == null) {
            showCreatePinScreen();
        }
    }

    public class ClickHandlers extends Activity {

        public void onClickLocalCurrency(View view) {
            showCurrency = !showCurrency;
            binding.setShowCurrency(showCurrency);
        }

        public void onClickShowSeed(View view) {
            clickedNewSeed = false;
            showSeed();
        }

        public void onClickShowNewSeed(View view) {
            clickedNewSeed = true;
            showSeed();
        }

        /*public void onClickChangeTheme(View view) {
            /* int current_theme = sharedPreferencesUtil.getTheme();
            if(current_theme == 1) {
                current_theme = 0;
            } else {
                current_theme = 1;
            } */
            //UIUtil.changeToTheme(this, current_theme);
            //clickedNewSeed = true;
            //showSeed();
        //}

        public void onClickLogOut(View view) {
            if (getActivity() instanceof WindowControl) {

                // show the logout are-you-sure dialog
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(getContext());
                }
                builder.setTitle(R.string.settings_logout_alert_title)
                        .setMessage(R.string.settings_logout_alert_message)
                        .setPositiveButton(R.string.settings_logout_alert_confirm_cta, (dialog, which) -> {
                            RxBus.get().post(new Logout());
                            dismiss();
                        })
                        .setNegativeButton(R.string.settings_logout_alert_cancel_cta, (dialog, which) -> {
                            // do nothing which dismisses the dialog
                        })
                        .show();
            }
        }
    }

    private void showCopySeedAlert() {
        // show the copy seed dialog
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }

        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            AlertDialog dialog = builder.setTitle(R.string.settings_seed_alert_title)
                    .setMessage(clickedNewSeed ? credentials.getNewlyGeneratedSeed().replaceAll("(.{4})", "$1 ") : credentials.getSeed().replaceAll("(.{4})", "$1 "))
                    .setPositiveButton(R.string.settings_seed_alert_confirm_cta, (d, which) -> {
                        analyticsService.track(AnalyticsEvents.SEED_COPIED);
                    })
                    .setIcon(R.drawable.ic_warning)
                    .show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        }

        KeyboardUtil.hideKeyboard(getActivity());
    }

    private void showFingerprintDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.settings_fingerprint_title));
        builder.setMessage(getString(R.string.settings_fingerprint_description));
        builder.setView(view);
        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText, (dialog, which) -> Reprint.cancelAuthentication());

        fingerprintDialog = builder.create();
        fingerprintDialog.setCanceledOnTouchOutside(false);
        // display dialog
        fingerprintDialog.show();
    }

    private void showFingerprintSuccess(View view) {
        if (isAdded()) {
            TextView textView = view.findViewById(R.id.fingerprint_textview);
            textView.setText(getString(R.string.settings_fingerprint_success));
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.dark_sky_blue));
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_success, 0, 0, 0);
            if (fingerprintDialog != null && fingerprintDialog.isShowing()) {
                fingerprintDialog.dismiss();
                showCopySeedAlert();
            }
        }
    }

    private void showFingerprintError(AuthenticationFailureReason reason, CharSequence message, View view) {
        if (isAdded()) {
            final HashMap<String, String> customData = new HashMap<>();
            customData.put("description", reason.name());
            analyticsService.track(AnalyticsEvents.SEED_COPY_FAILED, customData);
            TextView textView = view.findViewById(R.id.fingerprint_textview);
            textView.setText(message.toString());
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.error));
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_error, 0, 0, 0);
        }
    }
}
