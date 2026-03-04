package org.nikanikoo.flux.data.models;

import android.content.Context;

import org.json.JSONObject;
import org.nikanikoo.flux.R;

public class Group {
    private static Context appContext;
    private int id;
    private String name;
    private String screenName;
    private String description;
    private String photo50;
    private String photo100;
    private String photo200;
    private int membersCount;
    private int followersCount;
    private int topicsCount;
    private int photosCount;
    private int videosCount;
    private int audiosCount;
    private boolean isClosed;
    private boolean isAdmin;
    private boolean isMember;
    private boolean canPost;
    private String type;
    private String activity;
    private String status;
    private boolean verified;
    private String website;
    private String city;
    private String country;

    public Group() {}

    public static void setAppContext(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public Group(int id, String name, String photo50, int membersCount) {
        this.id = id;
        this.name = name;
        this.photo50 = photo50;
        this.membersCount = membersCount;
    }

    public static Group fromJson(JSONObject json) {
        Group group = new Group();
        try {
            System.out.println("Group.fromJson: Parsing JSON: " + json.toString());
            
            group.id = json.getInt("id");
            group.name = json.getString("name");
            group.screenName = json.optString("screen_name", "");
            group.description = json.optString("description", "");
            group.photo50 = json.optString("photo_50", "");
            group.photo100 = json.optString("photo_100", "");
            group.photo200 = json.optString("photo_200", "");
            group.membersCount = json.optInt("members_count", 0);
            
            // Счетчики (если есть)
            if (json.has("counters")) {
                JSONObject counters = json.getJSONObject("counters");
                group.followersCount = counters.optInt("followers", 0);
                group.topicsCount = counters.optInt("topics", 0);
                group.photosCount = counters.optInt("photos", 0);
                group.videosCount = counters.optInt("videos", 0);
                group.audiosCount = counters.optInt("audios", 0);
            }
            group.isClosed = json.optInt("is_closed", 0) == 1;
            group.isAdmin = json.optInt("is_admin", 0) == 1;
            group.isMember = json.optInt("is_member", 0) == 1;
            
            // can_post может быть boolean или int
            if (json.has("can_post")) {
                Object canPostValue = json.get("can_post");
                if (canPostValue instanceof Boolean) {
                    group.canPost = (Boolean) canPostValue;
                } else if (canPostValue instanceof Integer) {
                    group.canPost = ((Integer) canPostValue) == 1;
                } else {
                    group.canPost = json.optInt("can_post", 0) == 1;
                }
                System.out.println("Group.fromJson: can_post = " + group.canPost + " (type: " + canPostValue.getClass().getSimpleName() + ")");
            } else {
                group.canPost = false;
                System.out.println("Group.fromJson: can_post field not found, defaulting to false");
            }
            
            group.type = json.optString("type", "group");
            group.activity = json.optString("activity", "");
            group.status = json.optString("status", "");
            // verified может быть int 1/0 или boolean
if (json.has("verified")) {
    Object verifiedObj = json.opt("verified");
    if (verifiedObj instanceof Integer) {
        group.verified = (Integer) verifiedObj == 1;
    } else if (verifiedObj instanceof Boolean) {
        group.verified = (Boolean) verifiedObj;
    }
}
            group.website = json.optString("website", "");
            group.city = json.optString("city", "");
            group.country = json.optString("country", "");
            
            System.out.println("Group.fromJson: Successfully parsed group: " + group.getName() + " (ID: " + group.getId() + ", canPost: " + group.canPost + ", isAdmin: " + group.isAdmin + ", isMember: " + group.isMember + ", verified: " + group.verified + ")");
        } catch (Exception e) {
            System.err.println("Group.fromJson: Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }
        return group;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getScreenName() { return screenName; }
    public String getDescription() { return description; }
    public String getPhoto50() { return photo50; }
    public String getPhoto100() { return photo100; }
    public String getPhoto200() { return photo200; }
    public int getMembersCount() { return membersCount; }
    public int getFollowersCount() { return followersCount; }
    public int getTopicsCount() { return topicsCount; }
    public int getPhotosCount() { return photosCount; }
    public int getVideosCount() { return videosCount; }
    public int getAudiosCount() { return audiosCount; }
    public boolean isClosed() { return isClosed; }
    public boolean isAdmin() { return isAdmin; }
    public boolean isMember() { return isMember; }
    public boolean canPost() { return canPost; }
    public String getType() { return type; }
    public String getActivity() { return activity; }
    public String getStatus() { return status; }
    public boolean isVerified() { return verified; }
    public String getWebsite() { return website; }
    public String getCity() { return city; }
    public String getCountry() { return country; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public void setDescription(String description) { this.description = description; }
    public void setPhoto50(String photo50) { this.photo50 = photo50; }
    public void setPhoto100(String photo100) { this.photo100 = photo100; }
    public void setPhoto200(String photo200) { this.photo200 = photo200; }
    public void setMembersCount(int membersCount) { this.membersCount = membersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }
    public void setTopicsCount(int topicsCount) { this.topicsCount = topicsCount; }
    public void setPhotosCount(int photosCount) { this.photosCount = photosCount; }
    public void setVideosCount(int videosCount) { this.videosCount = videosCount; }
    public void setAudiosCount(int audiosCount) { this.audiosCount = audiosCount; }
    public void setClosed(boolean closed) { isClosed = closed; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    public void setMember(boolean member) { isMember = member; }
    public void setCanPost(boolean canPost) { this.canPost = canPost; }
    public void setType(String type) { this.type = type; }
    public void setActivity(String activity) { this.activity = activity; }
    public void setStatus(String status) { this.status = status; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public void setWebsite(String website) { this.website = website; }
    public void setCity(String city) { this.city = city; }
    public void setCountry(String country) { this.country = country; }

    public String getTypeDisplayName() {
        switch (type) {
            case "page": return "Страница";
            case "event": return "Мероприятие";
            default: return "Группа";
        }
    }

    public String getMembersCountText() {
        if (membersCount == 1) return getString(R.string.friend_1_follower);
        if (membersCount < 5) return membersCount + getString(R.string.friend_5_followers);
        return membersCount + getString(R.string.friend_followers6);
    }

    private String getString(int resId) {
        if (appContext != null) {
            return appContext.getString(resId);
        }
        return "";
    }
}