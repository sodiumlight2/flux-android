package org.nikanikoo.flux.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nikanikoo.flux.data.models.Audio;
import org.nikanikoo.flux.data.models.Video;

import java.util.ArrayList;
import java.util.List;

public class AttachmentProcessor {

    public static class AttachmentResult {
        private final List<String> imageUrls;
        private final List<String> imageMaxResUrls;
        private final List<Audio> audioAttachments;
        private final List<Video> videoAttachments;
        private final String unsupportedElementsText;

        public AttachmentResult(List<String> imageUrls, List<String> imageMaxResUrls, List<Audio> audioAttachments, List<Video> videoAttachments, String unsupportedElementsText) {
            this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
            this.imageMaxResUrls = imageMaxResUrls != null ? imageMaxResUrls : new ArrayList<>();
            this.audioAttachments = audioAttachments != null ? audioAttachments : new ArrayList<>();
            this.videoAttachments = videoAttachments != null ? videoAttachments : new ArrayList<>();
            this.unsupportedElementsText = unsupportedElementsText != null ? unsupportedElementsText : "";
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public List<String> getImageMaxResUrls() {
            return imageMaxResUrls;
        }

        public List<Audio> getAudioAttachments() {
            return audioAttachments;
        }

        public List<Video> getVideoAttachments() {
            return videoAttachments;
        }

        public String getUnsupportedElementsText() {
            return unsupportedElementsText;
        }
    }

    public static AttachmentResult processAttachments(JSONArray attachments) {
        List<String> imageUrls = new ArrayList<>();
        List<String> imageMaxResUrls = new ArrayList<>();
        List<Audio> audioAttachments = new ArrayList<>();
        List<Video> videoAttachments = new ArrayList<>();
        StringBuilder unsupportedElements = new StringBuilder();

        if (attachments == null || attachments.length() == 0) {
            return new AttachmentResult(imageUrls, imageMaxResUrls, audioAttachments, videoAttachments, "");
        }

        Logger.d("AttachmentProcessor", "Processing " + attachments.length() + " attachments");

        try {
            for (int i = 0; i < attachments.length(); i++) {
                JSONObject attachment = attachments.getJSONObject(i);
                String type = attachment.optString("type", "");

                Logger.d("AttachmentProcessor", "Processing attachment " + i + " of type: " + type);

                switch (type) {
                    case "photo":
                        if (attachment.has("photo")) {
                            JSONObject photo = attachment.getJSONObject("photo");
                            String url = ImageUtils.extractOptimalImageUrl(photo);
                            String maxResUrl = ImageUtils.extractMaxResImageUrl(photo);
                            if (!url.isEmpty()) {
                                imageUrls.add(url);
                                Logger.d("AttachmentProcessor", "Added photo (z): " + url);
                            }
                            if (!maxResUrl.isEmpty()) {
                                imageMaxResUrls.add(maxResUrl);
                                Logger.d("AttachmentProcessor", "Added photo (maxres): " + maxResUrl);
                            }
                        }
                        break;

                    case "audio":
                        if (attachment.has("audio")) {
                            JSONObject audioJson = attachment.getJSONObject("audio");
                            Audio audio = parseAudio(audioJson);
                            if (audio != null) {
                                audioAttachments.add(audio);
                                Logger.d("AttachmentProcessor", "Added audio: " + audio.getFullTitle());
                            }
                        }
                        break;

                    case "video":
                        if (attachment.has("video")) {
                            JSONObject videoJson = attachment.getJSONObject("video");
                            Video video = org.nikanikoo.flux.data.managers.VideoManager.parseVideo(videoJson);
                            if (video != null) {
                                videoAttachments.add(video);
                                Logger.d("AttachmentProcessor", "Added video: " + video.getTitle());
                            }
                        }
                        break;

                    case "doc":
                    case "poll":
                        String warning = getUnsupportedAttachmentWarning(type);
                        if (unsupportedElements.length() > 0) {
                            unsupportedElements.append("\n");
                        }
                        unsupportedElements.append(warning);
                        break;

                    default:
                        Logger.d("AttachmentProcessor", "Unknown attachment type: " + type);
                        break;
                }
            }
        } catch (Exception e) {
            Logger.e("AttachmentProcessor", "Error processing attachments", e);
        }

        Logger.d("AttachmentProcessor", "Processed attachments: " + imageUrls.size() + " images, " +
                 audioAttachments.size() + " audio, " + videoAttachments.size() + " videos, unsupported: " + unsupportedElements.toString());

        return new AttachmentResult(imageUrls, imageMaxResUrls, audioAttachments, videoAttachments, unsupportedElements.toString());
    }

    private static Audio parseAudio(JSONObject json) {
        try {
            Audio audio = new Audio();
            audio.setUniqueId(json.optString("unique_id", ""));
            audio.setId(json.optInt("id", 0));
            audio.setOwnerId(json.optInt("owner_id", 0));
            audio.setArtist(json.optString("artist", "Неизвестный исполнитель"));
            audio.setTitle(json.optString("title", "Без названия"));
            audio.setDuration(json.optInt("duration", 0));
            audio.setUrl(json.optString("url", ""));
            audio.setManifest(json.optString("manifest", ""));
            audio.setGenreId(json.optInt("genre_id", 0));
            audio.setGenreStr(json.optString("genre_str", ""));
            audio.setLyrics(json.optInt("lyrics", 0));
            audio.setAdded(json.optBoolean("added", false));
            audio.setEditable(json.optBoolean("editable", false));
            audio.setSearchable(json.optBoolean("searchable", true));
            audio.setExplicit(json.optBoolean("explicit", false));
            audio.setWithdrawn(json.optBoolean("withdrawn", false));
            audio.setReady(json.optBoolean("ready", true));
            return audio;
        } catch (Exception e) {
            Logger.e("AttachmentProcessor", "Error parsing audio object", e);
            return null;
        }
    }

    private static String getUnsupportedAttachmentWarning(String type) {
        switch (type) {
            case "doc":
                return "[Документ]";
            case "poll":
                return "[Опрос]";
            default:
                return "[Вложение: " + type + "]";
        }
    }
}
