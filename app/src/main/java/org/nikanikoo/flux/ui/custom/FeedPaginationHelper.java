package org.nikanikoo.flux.ui.custom;

import org.nikanikoo.flux.utils.Logger;

public class FeedPaginationHelper {
    private static final String TAG = "FeedPaginationHelper";

    private String nextFromCursor = null;
    
    private int itemsPerPage;
    private boolean hasMoreData = true;
    private boolean isLoading = false;
    private int totalItemsLoaded = 0;

    public FeedPaginationHelper(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        Logger.d(TAG, "FeedPaginationHelper created with itemsPerPage: " + itemsPerPage);
    }

    public String getNextFromCursor() {
        return nextFromCursor;
    }

    public void setNextFromCursor(String nextFrom) {
        this.nextFromCursor = nextFrom;
        Logger.d(TAG, "Set next_from cursor: " + nextFrom);
    }

    public void onDataLoaded(int itemsReceived, String newNextFrom) {
        Logger.d(TAG, "onDataLoaded called: itemsReceived=" + itemsReceived + 
            ", nextFrom=" + newNextFrom + ", isLoading=" + isLoading + 
            ", hasMoreData=" + hasMoreData);

        totalItemsLoaded += itemsReceived;
        isLoading = false;

        if (newNextFrom != null && !newNextFrom.isEmpty()) {
            this.nextFromCursor = newNextFrom;
        }

        if (itemsReceived == 0) {
            hasMoreData = false;
            Logger.d(TAG, "No more data available (received 0 items)");
        }

        if (newNextFrom == null || newNextFrom.isEmpty()) {
            hasMoreData = false;
            Logger.d(TAG, "No more data available (next_from is empty)");
        }

        Logger.d(TAG, "After onDataLoaded: nextFrom=" + nextFromCursor + 
            ", totalLoaded=" + totalItemsLoaded + 
            ", isLoading=" + isLoading + 
            ", hasMoreData=" + hasMoreData + 
            ", canLoadMore=" + canLoadMore());
    }

    public void onDataLoaded(int itemsReceived) {
        onDataLoaded(itemsReceived, null);
    }

    public void reset() {
        Logger.d(TAG, "Resetting pagination state");
        nextFromCursor = null;
        hasMoreData = true;
        isLoading = false;
        totalItemsLoaded = 0;
    }

    public void startLoading() {
        isLoading = true;
        Logger.d(TAG, "startLoading called: nextFrom=" + nextFromCursor + 
            ", isLoading=" + isLoading + 
            ", hasMoreData=" + hasMoreData + 
            ", canLoadMore=" + canLoadMore());
    }

    public void stopLoading() {
        isLoading = false;
        Logger.d(TAG, "Loading stopped");
    }

    public void setNoMoreData() {
        hasMoreData = false;
        isLoading = false;
        Logger.d(TAG, "No more data flag set");
    }

    public boolean hasMoreData() {
        return hasMoreData;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public int getTotalItemsLoaded() {
        return totalItemsLoaded;
    }

    public boolean canLoadMore() {
        return hasMoreData && !isLoading;
    }

    public boolean isFirstPage() {
        return nextFromCursor == null || nextFromCursor.isEmpty();
    }
}
