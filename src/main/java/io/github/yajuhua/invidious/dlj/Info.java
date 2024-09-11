package io.github.yajuhua.invidious.dlj;

import io.github.yajuhua.invidious.dlj.pojo.Video;
import io.github.yajuhua.invidious.dlj.pojo.com.Thumbnail;
import io.github.yajuhua.invidious.wrapper.api.Playlist;
import io.github.yajuhua.invidious.wrapper.api.Search;
import io.github.yajuhua.invidious.wrapper.api.Streams;
import io.github.yajuhua.invidious.wrapper.pojo.dto.*;
import io.github.yajuhua.invidious.wrapper.pojo.dto.commons.VideoDTO;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取封装后的info-json
 */
public class Info {

    /**
     * 单独video
     * @param url
     * @return
     */
    public static Video getVideo(String url) throws Exception {

        String videoId = getVideoId(url);
        VideoDetailDTO videoDetailDTO = io.github.yajuhua.invidious.wrapper.api.Video.get(videoId);
        Video video = new Video();
        //基本信息
        video.setId(videoDetailDTO.getVideoId());
        video.setTitle(videoDetailDTO.getTitle());
        video.setDescription(videoDetailDTO.getDescription());
        video.setTimestamp(videoDetailDTO.getPublished());

        //封面
        List<Thumbnail> thumbnails = new ArrayList<>();
        for (VideoDetailDTO.VideoThumbnailsDTO t1 : videoDetailDTO.getVideoThumbnails()) {
            Thumbnail thumbnail = new Thumbnail();
            thumbnail.setUrl(t1.getUrl());
            thumbnail.setResolution(t1.getWidth() + "x" + t1.getHeight());
            thumbnail.setHeight(t1.getHeight());
            thumbnail.setWidth(t1.getWidth());
            thumbnails.add(thumbnail);
        }
        video.setThumbnails(thumbnails);

        //视频格式
        List<Video.FormatEntry> formats = new ArrayList<>();
        for (VideoDetailDTO.AdaptiveFormatsDTO adaptiveFormat : videoDetailDTO.getAdaptiveFormats()) {
            Video.FormatEntry formatEntry = new Video.FormatEntry();
            formatEntry.setExt(adaptiveFormat.getContainer());
            formatEntry.setUrl(adaptiveFormat.getUrl());
            String encoding = adaptiveFormat.getType().split("\"")[1];
            if (adaptiveFormat.getType().startsWith("video")){
                formatEntry.setAcodec("none");
                formatEntry.setVcodec(encoding);
                formatEntry.setResolution(adaptiveFormat.getResolution());
            }else {
                formatEntry.setAcodec(encoding);
                formatEntry.setVcodec("none");
            }
            formats.add(formatEntry);
        }
        video.setFormats(formats);

        //字幕信息
        Map<String, List<Video.SubtitleEntry>> subtitles = new HashMap<>();
        URL dashUrl = new URL(videoDetailDTO.getDashUrl());
        List<VideoDetailDTO.CaptionsDTO> captions = videoDetailDTO.getCaptions();
        for (VideoDetailDTO.CaptionsDTO caption : captions) {
            List<Video.SubtitleEntry> subtitleEntries = new ArrayList<>();
            Video.SubtitleEntry entry = new Video.SubtitleEntry();
            entry.setExt("vtt");//youtube视频字幕应该都是vtt
            entry.setUrl(dashUrl.getProtocol() + "://" + dashUrl.getHost() + caption.getUrl());
            subtitleEntries.add(entry);
            subtitles.put(caption.getLabel(),subtitleEntries);
        }
        video.setSubtitles(subtitles);

        return video;
    }

    /**
     * 获取https://www.youtube.com/@username/videos 中视频列表
     * @param url
     * @param selectItems 选择集数
     * @return
     * @throws Exception
     */
    public static List<Video> getVideos(String url,List<Integer> selectItems) throws Exception {
        String channelId = getChannelId(url);
        VideosDTO videosDTO = io.github.yajuhua.invidious.wrapper.api.Videos.get(channelId);
        return toVideoList(videosDTO.getVideos(),selectItems);
    }

    /**
     * 获取https://www.youtube.com/@username/videos 中视频列表
     * 默认第一集
     * @param url
     * @return
     * @throws Exception
     */
    public static List<Video> getVideos(String url) throws Exception {
        return getVideos(url, Arrays.asList(1));
    }

    /**
     * 获取https://www.youtube.com/@username/streams 中视频列表
     * @param url
     * @param selectItems
     * @return
     */
    public static List<Video> getStreams(String url, List<Integer> selectItems) throws Exception {
        String channelId = getChannelId(url);
        StreamsDTO streamsDTO = Streams.get(channelId);
        return toVideoList(streamsDTO.getVideos(),selectItems);
    }

    /**
     * 获取https://www.youtube.com/@username/streams 中视频列表
     * 默认第一集
     * @param url
     * @return
     * @throws Exception
     */
    public static List<Video> getStreams(String url) throws Exception {
        return getStreams(url, Arrays.asList(1));
    }

    /**
     * 获取https://www.youtube.com/playlist?list=XXXX 中视频列表
     * @param url
     * @param selectItems
     * @return
     * @throws Exception
     */
    public static List<Video> getPlaylist(String url,List<Integer> selectItems) throws Exception {
        String playlistId = getPlaylistId(url);
        PlaylistDTO playlistDTO = Playlist.get(playlistId);
        return toVideoList(playlistDTO.getVideos(),selectItems);
    }

    /**
     * 获取https://www.youtube.com/playlist?list=XXXX 中视频列表
     * 默认第一集
     * @param url
     * @return
     * @throws Exception
     */
    public static List<Video> getPlaylist(String url) throws Exception {
        return getPlaylist(url, Arrays.asList(1));
    }

    /**
     * 将invidious中的视频列表转换成--load-info-json的格式
     * @param videoDTOS
     * @param selectItems
     * @return
     * @throws Exception
     */
    public static List<Video> toVideoList(List<VideoDTO> videoDTOS,List<Integer> selectItems) throws Exception {
        List<Video> videos = new ArrayList<>();
        if (!videoDTOS.isEmpty()){
            //视频列表 && 格式
            Collections.sort(selectItems);
            Integer maxIndex = selectItems.get(selectItems.size() - 1);
            if (maxIndex <= videoDTOS.size()){
                for (Integer index : selectItems) {
                    String videoId = videoDTOS.get(index - 1).getVideoId();
                    //https://www.youtube.com/watch?v=YLcz5hjqVLQ
                    Video video = Info.getVideo("https://www.youtube.com/watch?v=" + videoId);
                    videos.add(video);
                }
            }else {
                for (VideoDTO videoInfo : videoDTOS) {
                    //https://www.youtube.com/watch?v=YLcz5hjqVLQ
                    Video video = Info.getVideo("https://www.youtube.com/watch?v=" + videoInfo.getVideoId());
                    videos.add(video);
                }
            }
            return videos;
        }else {
            return videos;
        }
    }

    /**
     * 获取视频id
     * @param url
     * @return
     */
    public static String getVideoId(String url){
        try {
            Map<String, String> queryParams = getQueryParams(url);
            return queryParams.get("v");
        } catch (Exception e) {
            throw new RuntimeException("无法获取youtube视频id：" + e.getMessage());
        }
    }

    /**
     * 获取url中的参数
     * @param url
     * @return
     * @throws Exception
     */
    public static Map<String, String> getQueryParams(String url) throws Exception{
        URI uri = new URI(url);
        String query = uri.getQuery();
        Map<String, String> queryParams = new HashMap<>();

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                queryParams.put(key, value);
            }
        }

        return queryParams;
    }

    /**
     * 获取playlistId
     * @param url
     * @return
     */
    public static String getPlaylistId(String url){
        try {
            Map<String, String> queryParams = getQueryParams(url);
            String list = queryParams.get("list");
            return list.trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取channelId
     * @param url
     * @return
     */
    public static String getChannelId(String url){
        try {
            url = URLDecoder.decode(url);
            // 正则表达式匹配 @username 并提取用户名
            Pattern patternUsername = Pattern.compile("@([\\p{L}a-zA-Z0-9_]+)");
            Matcher matcherUsername = patternUsername.matcher(url);

            // 正则表达式匹配 /channel/ 之后的部分直到下一个斜杠或结尾
            Pattern patternId = Pattern.compile("channel/([a-zA-Z0-9_-]+)");
            Matcher matcherId = patternId.matcher(url);

            String username = null;
            if (matcherUsername.find()){
                username = "@" + matcherUsername.group(1);
            } else if (matcherId.find()) {
                return matcherId.group(1);
            }else {
                throw new RuntimeException("无法获取channelId");
            }
            //https://invidious.private.coffee/api/v1/search?q=@bulianglin&type=channel
            List<SearchDTO> searchDTOS = Search.get(username);
            if (searchDTOS.isEmpty()){
                throw new RuntimeException("无法获取channelId");
            }else {
                return searchDTOS.get(0).getAuthorId();
            }
        } catch (Exception e) {
            throw new RuntimeException("无法获取channelId");
        }
    }

    /**
     * 支持下载invidious链接
     * @return如果主机是www.youtube.com则返回Null
     */
    public static String getInvidiousInstanceFromUrl(String url) throws Exception {
        URL url1 = new URL(url);
        if (url1.getHost().contains("youtube.com")){
            return null;
        }else {
            return url1.getProtocol() + "://" + url1.getHost();
        }
    }
}
