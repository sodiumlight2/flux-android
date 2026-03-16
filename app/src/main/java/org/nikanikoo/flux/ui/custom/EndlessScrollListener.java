package org.nikanikoo.flux.ui.custom;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.nikanikoo.flux.utils.Logger;

/**
 * Улучшенный слушатель бесконечного скролла для RecyclerView
 * Работает совместно с PaginationHelper для корректной пагинации
 */
public abstract class EndlessScrollListener extends RecyclerView.OnScrollListener {
    private static final String TAG = "EndlessScrollListener";

    // Минимальное количество элементов до конца списка для начала загрузки
    private static final int VISIBLE_THRESHOLD = 5;
    // Задержка между загрузками для предотвращения спама запросов
    private static final long LOADING_DELAY_MS = 500;

    private final LinearLayoutManager layoutManager;
    private final PaginationHelper paginationHelper;
    private final FeedPaginationHelper feedPaginationHelper;
    private int previousTotalItemCount = 0;
    private long lastLoadTime = 0;
    private boolean isEnabled = true;

    public EndlessScrollListener(LinearLayoutManager layoutManager, PaginationHelper paginationHelper) {
        this.layoutManager = layoutManager;
        this.paginationHelper = paginationHelper;
        this.feedPaginationHelper = null;
        Logger.d(TAG, "EndlessScrollListener initialized with PaginationHelper (offset-based)");
    }

    public EndlessScrollListener(LinearLayoutManager layoutManager, FeedPaginationHelper feedPaginationHelper) {
        this.layoutManager = layoutManager;
        this.paginationHelper = null;
        this.feedPaginationHelper = feedPaginationHelper;
        Logger.d(TAG, "EndlessScrollListener initialized with FeedPaginationHelper (cursor-based)");
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        if (!isEnabled) {
            return;
        }

        if (!canLoadMore()) {
            Logger.d(TAG, "Cannot load more: isLoading=" + isLoading() + ", hasMoreData=" + hasMoreData());
            return;
        }

        // Загружаем только при скролле вниз
        if (dy <= 0) return;

        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        int totalItemCount = layoutManager.getItemCount();

        // Проверяем, завершилась ли предыдущая загрузка
        if (isLoading() && (totalItemCount > previousTotalItemCount)) {
            previousTotalItemCount = totalItemCount;
            Logger.d(TAG, "Loading completed, total items: " + totalItemCount);
        }

        // Проверяем условия для загрузки новых данных
        if (shouldLoadMore(lastVisibleItemPosition, totalItemCount)) {
            long currentTime = System.currentTimeMillis();

            // Предотвращаем частые запросы
            if (currentTime - lastLoadTime > LOADING_DELAY_MS) {
                Logger.d(TAG, "Triggering load more, total=" + totalItemCount + 
                    ", lastVisible=" + lastVisibleItemPosition);
                onLoadMore(getCurrentOffset(), totalItemCount, view);
                startLoading();
                lastLoadTime = currentTime;
            } else {
                Logger.d(TAG, "Skipping load due to delay, time since last load: " + (currentTime - lastLoadTime) + "ms");
            }
        }
    }

    private boolean shouldLoadMore(int lastVisibleItemPosition, int totalItemCount) {
        return (lastVisibleItemPosition + VISIBLE_THRESHOLD) >= totalItemCount
               && totalItemCount > 0;
    }

    /**
     * Сброс состояния при обновлении списка
     */
    public void resetState() {
        Logger.d(TAG, "Resetting state");
        previousTotalItemCount = 0;
        lastLoadTime = 0;
        if (paginationHelper != null) {
            paginationHelper.reset();
        } else if (feedPaginationHelper != null) {
            feedPaginationHelper.reset();
        }
    }

    /**
     * Включение/отключение слушателя
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        Logger.d(TAG, "Enabled set to: " + enabled);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getCurrentOffset() {
        if (paginationHelper != null) {
            return paginationHelper.getCurrentOffset();
        }
        return 0;
    }

    public boolean hasMoreData() {
        if (paginationHelper != null) {
            return paginationHelper.hasMoreData();
        } else if (feedPaginationHelper != null) {
            return feedPaginationHelper.hasMoreData();
        }
        return false;
    }

    public boolean isLoading() {
        if (paginationHelper != null) {
            return paginationHelper.isLoading();
        } else if (feedPaginationHelper != null) {
            return feedPaginationHelper.isLoading();
        }
        return false;
    }

    public boolean canLoadMore() {
        if (paginationHelper != null) {
            return paginationHelper.canLoadMore();
        } else if (feedPaginationHelper != null) {
            return feedPaginationHelper.canLoadMore();
        }
        return false;
    }

    public void startLoading() {
        if (paginationHelper != null) {
            paginationHelper.startLoading();
        } else if (feedPaginationHelper != null) {
            feedPaginationHelper.startLoading();
        }
    }

    /**
     * Абстрактный метод для загрузки данных
     * @param offset текущий offset для API запроса
     * @param totalItemsCount общее количество элементов в адаптере
     * @param view RecyclerView
     */
    public abstract void onLoadMore(int offset, int totalItemsCount, RecyclerView view);
}
