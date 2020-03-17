package org.magnum.dataup.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.VideoFileManager;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

    private List<Video> videoCollection = new ArrayList<>();
    private static final AtomicLong currentId = new AtomicLong(0L);
    private static final String host = "http://localhost:8080";
    private static final String port = "8080";

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    public @ResponseBody List<Video> getUploadedVideos() {
        return videoCollection;
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video video) {
        final long id = currentId.incrementAndGet();
        video.setId(id);
        video.setDataUrl(host + ":" + port + "/video/" + id + "/data");
        videoCollection.add(video);
        return video;
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    public @ResponseBody VideoStatus uploadVideoData(
            @PathVariable("id") long videoId, @RequestParam("data") MultipartFile videoData, HttpServletResponse response) throws IOException {

        InputStream inputStream = videoData.getInputStream();
        final Video video = getVideoWithId(videoId);
        if (video == null) {
            response.sendError(404);
            return new VideoStatus(VideoState.PROCESSING);
        }

        getVideoFileManager().saveVideoData(video, inputStream);
        inputStream.close();
        return new VideoStatus(VideoState.READY);
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
    public @ResponseBody HttpServletResponse getVideo(@PathVariable("id") long videoId,
                                                      HttpServletResponse response) throws IOException {

        Video video = getVideoWithId(videoId);

        if (video == null) {
            response.sendError(404);
            return response;
        }

        final ServletOutputStream outputStream = response.getOutputStream();
        getVideoFileManager().copyVideoData(video, outputStream);
        outputStream.close();
        return response;
    }

    private VideoFileManager getVideoFileManager() throws IOException {
        return Objects.requireNonNull(VideoFileManager.get());
    }

    private Video getVideoWithId(long id) {
        for (Video v : videoCollection) {
            if (v.getId() == id) {
                return v;
            }
        }
        return null;
    }
}