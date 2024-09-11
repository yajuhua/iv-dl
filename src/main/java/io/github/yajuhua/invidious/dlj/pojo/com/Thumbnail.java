package io.github.yajuhua.invidious.dlj.pojo.com;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封面
 */
@NoArgsConstructor
@Data
public class Thumbnail {

    @SerializedName("url")
    private String url;
    @SerializedName("height")
    private Integer height;
    @SerializedName("width")
    private Integer width;
    @SerializedName("resolution")
    private String resolution;
}
