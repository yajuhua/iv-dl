package io.github.yajuhua.invidious.dlj.pojo;

import io.github.yajuhua.invidious.dlj.pojo.com.Thumbnail;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 单个视频的
 */
@NoArgsConstructor
@Data
public class Video {

    private String id;
    private String title;
    private List<FormatEntry> formats;
    private Map<String, List<SubtitleEntry>> subtitles;
    private Integer timestamp;
    private String description;
    private List<Thumbnail> thumbnails;
    @Data
    @NoArgsConstructor
     public static class FormatEntry {
            private String url;
            private String ext;
            private String vcodec;
            private String acodec;
            private String resolution;
        }

        @Data
        @NoArgsConstructor
        public static class SubtitleEntry {
            private String ext;
            private String url;
        }
    }
