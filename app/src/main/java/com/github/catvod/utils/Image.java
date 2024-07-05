package com.github.catvod.utils;

public class Image {

    public static final String FOLDER = "https://gitee.com/yx-sy/img/raw/main/cus/folder.png";
    public static final String VIDEO = "https://gitee.com/yx-sy/img/raw/main/cus/video.png";
    public static final String PUSH = "https://gitee.com/yx-sy/img/raw/main/cus/push.png";

    public static String getIcon(boolean folder) {
        return folder ? FOLDER : VIDEO;
    }
}
