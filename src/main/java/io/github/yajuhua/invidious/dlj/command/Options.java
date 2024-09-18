package io.github.yajuhua.invidious.dlj.command;

import io.github.yajuhua.invidious.dlj.command.annotations.CmdOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Options {
    @CmdOption(filter = true)
    public String proxy;

    @CmdOption("playlist-start")
    public Integer playlistStart;

    @CmdOption("playlist-end")
    public Integer playlistEnd;

    @CmdOption("playlist-items")
    public List<Integer> playlistItems;

    @CmdOption(value = "no-warnings",filter = true)
    public boolean noWarnings;

    @CmdOption(value = "batch-file",shortForm = "a")
    public String batchFile;

    @CmdOption(value = "iv-dl-version")
    public boolean ivDlVersion;

    @CmdOption(value = "yt-dlp-path")
    public String ytDlpPath = "yt-dlp";

    @CmdOption(shortForm = "v",filter = true)
    public boolean verbose;

    @CmdOption(shortForm = "h",filter = true)
    public boolean help;

    @CmdOption(filter = true)
    public boolean version;

    @CmdOption(value = "iv-dl-help")
    public boolean ivDlHelp;

    @CmdOption
    public boolean test;
}
