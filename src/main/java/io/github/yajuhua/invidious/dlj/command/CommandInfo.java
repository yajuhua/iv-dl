package io.github.yajuhua.invidious.dlj.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CommandInfo {
    private Options options;//选项
    private List<String> filter;//需要传递给yt-dlp的
}
