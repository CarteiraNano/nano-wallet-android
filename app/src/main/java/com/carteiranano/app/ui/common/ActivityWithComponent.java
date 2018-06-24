package com.carteiranano.app.ui.common;

import com.carteiranano.app.di.activity.ActivityComponent;
import com.carteiranano.app.di.application.ApplicationComponent;

/**
 * Interface for Activity with a Component
 */

public interface ActivityWithComponent {
    ActivityComponent getActivityComponent();
    ApplicationComponent getApplicationComponent();
}
