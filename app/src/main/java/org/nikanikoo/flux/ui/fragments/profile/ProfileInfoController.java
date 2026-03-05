package org.nikanikoo.flux.ui.fragments.profile;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.UserProfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Controller для управления отображением информации профиля.
 * Инкапсулирует логику обновления UI элементов профиля.
 */
public class ProfileInfoController {
    
    // Основные View
    private ImageView profileAvatarLarge;
    private TextView profileNameLarge;
    private ImageView profileVerified;
    private TextView profileOnline;
    private TextView profileStatus;
    
    // Счетчики
    private TextView friendsCount;
    private TextView followersCount;
    private TextView groupsCount;
    private TextView photosCount;
    private TextView videosCount;
    private TextView audiosCount;
    
    // Детали профиля
    private TextView profileId;
    private TextView profileScreenName;
    private TextView profileSex;
    private TextView profileRegistrationDate;
    private TextView profileLastSeen;
    private TextView profileCity;
    private TextView profileEmail;
    private TextView profileTelegram;
    private TextView profileAbout;
    private TextView profileInterests;
    private TextView profileMusic;
    private TextView profileMovies;
    private TextView profileBooks;
    private TextView profileQuotes;
    
    // Layout'ы для скрытия/показа
    private LinearLayout screenNameLayout;
    private LinearLayout sexLayout;
    private LinearLayout registrationDateLayout;
    private LinearLayout lastSeenLayout;
    private LinearLayout cityLayout;
    private LinearLayout emailLayout;
    private LinearLayout telegramLayout;
    private LinearLayout aboutLayout;
    private LinearLayout interestsLayout;
    private LinearLayout musicLayout;
    private LinearLayout moviesLayout;
    private LinearLayout booksLayout;
    private LinearLayout quotesLayout;
    
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dateTimeFormat;
    private Context context;
    
    public ProfileInfoController(View rootView) {
        this.context = rootView.getContext().getApplicationContext();
        initViews(rootView);
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }
    
    private void initViews(View view) {
        // Основные View
        profileAvatarLarge = view.findViewById(R.id.profile_avatar_large);
        profileNameLarge = view.findViewById(R.id.profile_name_large);
        profileVerified = view.findViewById(R.id.profile_verified);
        profileOnline = view.findViewById(R.id.profile_online);
        profileStatus = view.findViewById(R.id.profile_status);
        
        // Счетчики
        friendsCount = view.findViewById(R.id.friends_count);
        followersCount = view.findViewById(R.id.followers_count);
        groupsCount = view.findViewById(R.id.groups_count);
        photosCount = view.findViewById(R.id.photos_count);
        videosCount = view.findViewById(R.id.videos_count);
        audiosCount = view.findViewById(R.id.audios_count);
        
        // Детали профиля
        profileId = view.findViewById(R.id.profile_id);
        profileScreenName = view.findViewById(R.id.profile_screen_name);
        profileSex = view.findViewById(R.id.profile_sex);
        profileRegistrationDate = view.findViewById(R.id.profile_registration_date);
        profileLastSeen = view.findViewById(R.id.profile_last_seen);
        profileCity = view.findViewById(R.id.profile_city);
        profileEmail = view.findViewById(R.id.profile_email);
        profileTelegram = view.findViewById(R.id.profile_telegram);
        profileAbout = view.findViewById(R.id.profile_about);
        profileInterests = view.findViewById(R.id.profile_interests);
        profileMusic = view.findViewById(R.id.profile_music);
        profileMovies = view.findViewById(R.id.profile_movies);
        profileBooks = view.findViewById(R.id.profile_books);
        profileQuotes = view.findViewById(R.id.profile_quotes);
        
        // Layout'ы
        screenNameLayout = view.findViewById(R.id.screen_name_layout);
        sexLayout = view.findViewById(R.id.sex_layout);
        registrationDateLayout = view.findViewById(R.id.registration_date_layout);
        lastSeenLayout = view.findViewById(R.id.last_seen_layout);
        cityLayout = view.findViewById(R.id.city_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        telegramLayout = view.findViewById(R.id.telegram_layout);
        aboutLayout = view.findViewById(R.id.about_layout);
        interestsLayout = view.findViewById(R.id.interests_layout);
        musicLayout = view.findViewById(R.id.music_layout);
        moviesLayout = view.findViewById(R.id.movies_layout);
        booksLayout = view.findViewById(R.id.books_layout);
        quotesLayout = view.findViewById(R.id.quotes_layout);
    }
    
    /**
     * Обновить всю информацию профиля
     */
    public void updateProfileInfo(UserProfile profile) {
        if (profile == null) {
            return;
        }
        
        updateBasicInfo(profile);
        updateCounters(profile);
        updateDetailedInfo(profile);
    }
    
    /**
     * Обновить основную информацию (имя, статус, аватар)
     */
    private void updateBasicInfo(UserProfile profile) {
        if (profileNameLarge != null) {
            profileNameLarge.setText(profile.getFullName());
        }
        
        if (profileVerified != null) {
            profileVerified.setVisibility(profile.isVerified() ? View.VISIBLE : View.GONE);
        }
        
        if (profileOnline != null) {
            profileOnline.setText(profile.isProfileOnline() ? "Online" : "Offline");
            profileOnline.setVisibility(View.VISIBLE);
        }
        
        if (profileStatus != null) {
            String status = profile.getProfileStatus();
            profileStatus.setText(status);
            profileStatus.setVisibility(status != null && !status.isEmpty() ? View.VISIBLE : View.GONE);
        }
        
        // Загрузка аватара
        if (profileAvatarLarge != null) {
            String photoUrl = profile.getPhoto200();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.camera_200)
                        .error(R.drawable.camera_200)
                        .into(profileAvatarLarge);
            }
        }
    }
    
    /**
     * Обновить счетчики
     */
    private void updateCounters(UserProfile profile) {
        setTextSafe(friendsCount, profile.getFriendsCount());
        setTextSafe(followersCount, profile.getFollowersCount());
        setTextSafe(groupsCount, profile.getGroupsCount());
        setTextSafe(photosCount, profile.getPhotosCount());
        setTextSafe(videosCount, profile.getVideosCount());
        setTextSafe(audiosCount, profile.getAudiosCount());
    }
    
    /**
     * Обновить детальную информацию
     */
    private void updateDetailedInfo(UserProfile profile) {
        // ID
        if (profileId != null) {
            profileId.setText(String.valueOf(profile.getId()));
        }
        
        // Screen name
        setTextWithLayout(profileScreenName, screenNameLayout, profile.getScreenName(), 
                value -> "@" + value);
        
        // Пол
        setSexInfo(profile.getSex());
        
        // Дата регистрации
        setDateWithLayout(profileRegistrationDate, registrationDateLayout, 
                profile.getRegDate(), false);
        
        // Последняя активность
        setDateWithLayout(profileLastSeen, lastSeenLayout, 
                profile.getLastSeen(), true);
        
        // Город
        setTextWithLayout(profileCity, cityLayout, profile.getCity(), null);
        
        // Email
        setTextWithLayout(profileEmail, emailLayout, profile.getEmail(), null);
        
        // Telegram
        setTextWithLayout(profileTelegram, telegramLayout, profile.getTelegram(), 
                value -> "@" + value);
        
        // О себе
        setTextWithLayout(profileAbout, aboutLayout, profile.getAbout(), null);
        
        // Интересы
        setTextWithLayout(profileInterests, interestsLayout, profile.getInterests(), null);
        
        // Музыка
        setTextWithLayout(profileMusic, musicLayout, profile.getMusic(), null);
        
        // Фильмы
        setTextWithLayout(profileMovies, moviesLayout, profile.getMovies(), null);
        
        // Книги
        setTextWithLayout(profileBooks, booksLayout, profile.getBooks(), null);
        
        // Цитаты
        setTextWithLayout(profileQuotes, quotesLayout, profile.getQuotes(), null);
    }
    
    /**
     * Безопасно установить текст счетчика
     */
    private void setTextSafe(TextView textView, int value) {
        if (textView != null) {
            textView.setText(String.valueOf(value));
        }
    }
    
    /**
     * Установить текст с проверкой на null/empty и управлением видимостью layout
     */
    private void setTextWithLayout(TextView textView, LinearLayout layout, 
                                   String value, TextFormatter formatter) {
        if (textView == null || layout == null) {
            return;
        }
        
        if (value != null && !value.isEmpty() && !"null".equals(value)) {
            textView.setText(formatter != null ? formatter.format(value) : value);
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Установить информацию о поле
     */
    private void setSexInfo(int sex) {
        if (profileSex == null || sexLayout == null) {
            return;
        }
        
        if (sex == 1) {
            profileSex.setText(context.getString(R.string.profile_sex_male));
            sexLayout.setVisibility(View.VISIBLE);
        } else if (sex == 2) {
            profileSex.setText(context.getString(R.string.profile_sex_female));
            sexLayout.setVisibility(View.VISIBLE);
        } else {
            sexLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Установить дату с форматированием
     */
    private void setDateWithLayout(TextView textView, LinearLayout layout, 
                                   long timestamp, boolean includeTime) {
        if (textView == null || layout == null) {
            return;
        }
        
        if (timestamp > 0) {
            SimpleDateFormat format = includeTime ? dateTimeFormat : dateFormat;
            textView.setText(format.format(new Date(timestamp * 1000)));
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Получить View аватара для обработки кликов
     */
    public ImageView getAvatarView() {
        return profileAvatarLarge;
    }
    
    /**
     * Интерфейс для форматирования текста
     */
    private interface TextFormatter {
        String format(String value);
    }
}
