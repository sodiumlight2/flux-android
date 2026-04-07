package org.nikanikoo.flux.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nikanikoo.flux.data.models.UserProfile;
import org.nikanikoo.flux.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class AccountManager {
    private static final String TAG = "AccountManager";
    private static final String PREF_NAME = "accounts_prefs";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_CURRENT_ACCOUNT_ID = "current_account_id";

    private final SharedPreferences prefs;
    private static AccountManager instance;

    private AccountManager(Context context) {
        SharedPreferences tempPrefs;

        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            tempPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            Logger.d(TAG, "AccountManager using EncryptedSharedPreferences");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize encrypted storage for accounts, falling back to plain SharedPreferences", e);
            tempPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        this.prefs = tempPrefs;
    }
    
    public static synchronized AccountManager getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Account data class
     */
    public static class Account {
        public final String id;
        public final String token;
        public final String instance;
        public final String userId;
        public String fullName;
        public String screenName;
        public String photoUrl;
        public boolean isVerified;
        
        public Account(String id, String token, String instance, String userId) {
            this.id = id;
            this.token = token;
            this.instance = instance;
            this.userId = userId;
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("token", token);
            json.put("instance", instance);
            json.put("userId", userId);
            json.put("fullName", fullName);
            json.put("screenName", screenName);
            json.put("photoUrl", photoUrl);
            json.put("isVerified", isVerified);
            return json;
        }
        
        public static Account fromJson(JSONObject json) throws JSONException {
            Account account = new Account(
                json.getString("id"),
                json.getString("token"),
                json.getString("instance"),
                json.getString("userId")
            );
            account.fullName = json.optString("fullName", "");
            account.screenName = json.optString("screenName", "");
            account.photoUrl = json.optString("photoUrl", "");
            account.isVerified = json.optBoolean("isVerified", false);
            return account;
        }
    }
    
    /**
     * Add a new account or update existing one
     */
    public void addAccount(String token, String instance, UserProfile profile) {
        try {
            List<Account> accounts = getAccounts();
            
            // Check if account already exists (same userId + instance)
            String userId = String.valueOf(profile.getId());
            Account existingAccount = findAccount(userId, instance, accounts);
            
            if (existingAccount != null) {
                // Update existing account
                accounts.remove(existingAccount);
            }
            
            // Create new account
            Account account = new Account(
                existingAccount != null ? existingAccount.id : UUID.randomUUID().toString(),
                token,
                instance,
                userId
            );
            account.fullName = profile.getFullName();
            account.screenName = profile.getScreenName();
            account.photoUrl = profile.getPhoto200();
            account.isVerified = profile.isVerified();
            
            accounts.add(account);
            saveAccounts(accounts);
            
            // Set as current account
            setCurrentAccountId(account.id);
            
            Logger.d(TAG, "Added/Updated account: " + account.fullName + " @ " + instance);
        } catch (Exception e) {
            Logger.e(TAG, "Error adding account", e);
        }
    }
    
    /**
     * Find account by userId and instance
     */
    private Account findAccount(String userId, String instance, List<Account> accounts) {
        for (Account account : accounts) {
            if (account.userId.equals(userId) && account.instance.equals(instance)) {
                return account;
            }
        }
        return null;
    }
    
    /**
     * Get all saved accounts
     */
    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        try {
            String accountsJson = prefs.getString(KEY_ACCOUNTS, "[]");
            JSONArray array = new JSONArray(accountsJson);
            for (int i = 0; i < array.length(); i++) {
                accounts.add(Account.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error loading accounts", e);
        }
        return accounts;
    }
    
    /**
     * Save accounts list
     */
    private void saveAccounts(List<Account> accounts) {
        try {
            JSONArray array = new JSONArray();
            for (Account account : accounts) {
                array.put(account.toJson());
            }
            String jsonString = array.toString();
            Logger.d(TAG, "Saving accounts JSON: " + jsonString);
            prefs.edit().putString(KEY_ACCOUNTS, jsonString).apply();
        } catch (Exception e) {
            Logger.e(TAG, "Error saving accounts", e);
        }
    }
    
    /**
     * Get current active account ID
     */
    public String getCurrentAccountId() {
        return prefs.getString(KEY_CURRENT_ACCOUNT_ID, null);
    }
    
    /**
     * Set current active account
     */
    public void setCurrentAccountId(String accountId) {
        prefs.edit().putString(KEY_CURRENT_ACCOUNT_ID, accountId).apply();
    }
    
    /**
     * Get current account
     */
    public Account getCurrentAccount() {
        String currentId = getCurrentAccountId();
        if (currentId == null) return null;
        
        for (Account account : getAccounts()) {
            if (account.id.equals(currentId)) {
                return account;
            }
        }
        return null;
    }
    
    /**
     * Switch to a different account
     */
    public boolean switchToAccount(String accountId) {
        for (Account account : getAccounts()) {
            if (account.id.equals(accountId)) {
                setCurrentAccountId(accountId);
                Logger.d(TAG, "Switched to account: " + account.fullName);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Remove an account
     */
    public void removeAccount(String accountId) {
        List<Account> accounts = getAccounts();
        Account toRemove = null;
        for (Account account : accounts) {
            if (account.id.equals(accountId)) {
                toRemove = account;
                break;
            }
        }
        
        if (toRemove != null) {
            accounts.remove(toRemove);
            saveAccounts(accounts);
            
            // If we removed the current account, switch to another one
            if (accountId.equals(getCurrentAccountId())) {
                if (!accounts.isEmpty()) {
                    setCurrentAccountId(accounts.get(0).id);
                } else {
                    prefs.edit().remove(KEY_CURRENT_ACCOUNT_ID).apply();
                }
            }
            
            Logger.d(TAG, "Removed account: " + toRemove.fullName);
        }
    }
    
    /**
     * Update account profile data
     */
    public void updateAccountProfile(String accountId, UserProfile profile) {
        List<Account> accounts = getAccounts();
        for (Account account : accounts) {
            if (account.id.equals(accountId)) {
                account.fullName = profile.getFullName();
                account.screenName = profile.getScreenName();
                account.photoUrl = profile.getPhoto200();
                account.isVerified = profile.isVerified();
                saveAccounts(accounts);
                Logger.d(TAG, "Updated profile for account: " + account.fullName);
                break;
            }
        }
    }
    
    /**
     * Check if there are multiple accounts
     */
    public boolean hasMultipleAccounts() {
        return getAccounts().size() > 1;
    }
    
    /**
     * Get account count
     */
    public int getAccountCount() {
        return getAccounts().size();
    }
    
    /**
     * Clear all accounts (logout all)
     */
    public void clearAllAccounts() {
        prefs.edit().clear().apply();
        Logger.d(TAG, "Cleared all accounts");
    }
}
