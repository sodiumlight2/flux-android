package org.nikanikoo.flux.ui.activities;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import org.nikanikoo.flux.utils.ErrorViewHandler;
import org.nikanikoo.flux.utils.Logger;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    protected ErrorViewHandler errorViewHandler;
    protected View mainContent;

    protected void setupErrorView(int mainContentId) {
        mainContent = findViewById(mainContentId);
        if (mainContent != null && mainContent.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) mainContent.getParent();
            errorViewHandler = new ErrorViewHandler(this, parent, mainContent);
        } else {
            Logger.w(TAG, "setupErrorView: mainContent not found or parent is not ViewGroup, id: " + mainContentId);
        }
    }

    protected void setupErrorViewWithView(@NonNull View errorView) {
        this.errorViewHandler = new ErrorViewHandler(this, errorView);
    }

    protected void showError(@NonNull ErrorViewHandler.ErrorType type) {
        if (errorViewHandler != null) {
            errorViewHandler.showError(type);
        }
    }

    protected void showError(@StringRes int titleRes, @StringRes int messageRes) {
        if (errorViewHandler != null) {
            errorViewHandler.showError(titleRes, messageRes);
        }
    }

    protected void showError(@NonNull String title, @NonNull String message) {
        if (errorViewHandler != null) {
            errorViewHandler.showError(title, message);
        }
    }

    protected void showErrorAuto(@NonNull String errorMessage) {
        if (errorViewHandler != null) {
            errorViewHandler.showErrorAuto(errorMessage);
        }
    }

    protected void hideError() {
        if (errorViewHandler != null) {
            errorViewHandler.hideError();
        }
    }

    protected void setRetryCallback(ErrorViewHandler.RetryCallback callback) {
        if (errorViewHandler != null) {
            errorViewHandler.setRetryCallback(callback);
        }
    }

    @Nullable
    protected ErrorViewHandler getErrorViewHandler() {
        return errorViewHandler;
    }
}
