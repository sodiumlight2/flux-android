package org.nikanikoo.flux.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import org.nikanikoo.flux.utils.ErrorViewHandler;
import org.nikanikoo.flux.utils.Logger;

public abstract class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    protected ErrorViewHandler errorViewHandler;
    protected View rootView;
    protected View mainContent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // subclasses must call setupErrorView(view, mainContentId)
    }

    protected void setupErrorView(@NonNull View view, int mainContentId) {
        this.rootView = view;
        this.mainContent = view.findViewById(mainContentId);
        if (mainContent != null) {
            if (view instanceof ViewGroup) {
                errorViewHandler = new ErrorViewHandler(requireContext(), (ViewGroup) view, mainContent);
            }
        } else {
            Logger.w(TAG, "setupErrorView: mainContent not found with id: " + mainContentId);
        }
    }

    protected void setupErrorViewWithView(@NonNull View errorView) {
        this.errorViewHandler = new ErrorViewHandler(requireContext(), errorView);
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
