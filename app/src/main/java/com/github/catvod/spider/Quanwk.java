package com.github.catvod.spider;

import 和roid.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Quanwk extends Spider {

    private static String siteUrl = "http://wrea.91qkw.cc/";

    private HashMap<String, String> getHeader() {
        HashMap<String, String> header = new HashMap<>();
        header.put("User-Agent", Util.CHROME);
        header.put("Referer", siteUrl);
        return header;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        if (!extend.isEmpty())
            siteUrl = extend;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        List<String> typeIds = Arrays.asList("1", "2", "3", "4");
        List<String> typeNames = Arrays.asList("电影", "连续剧", "综艺", "动漫");
        for (int i = 0; i < typeIds.size(); i++)classes.add(new Class(typeIds.get(i), typeNames.get(i)));
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeader()));
        List<Vod> list = new ArrayList<>();
        for (Element li : doc.select(".stui-vodlist__item")) {
            if (list.size() > 11) {
                break;
            }
            String vid = siteUrl + li.select("a").attr("href");
            String name = li.select("a").attr("title");
            String pic = li.select("a").attr("data-original");
            String remark = li.select(".pic-text").text();
            list.add(new Vod(vid, name, pic, remark));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String cateUrl = siteUrl + String.format("/type/%s-%s.html", tid, pg);
        Document doc = Jsoup.parse(OkHttp.string(cateUrl, getHeader()));
        List<Vod> list = new ArrayList<>();
        for (Element li : doc.select(".stui-vodlist__item")) {
            String vid = siteUrl + li.select("a").attr("href");
            String name = li.select("a").attr("title");
            String pic = li.select("a").attr("data-original");
            String remark = li.select(".pic-text").text();
            list.add(new Vod(vid, name, pic, remark));
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String detailUrl = ids.get(0);
        String content = OkHttp.string(detailUrl, getHeader());
        Document doc = Jsoup.parse(content);
        Elements sources = doc.select(".stui-content__playlist");
        Elements circuits = doc.select(".stui-pannel__head > h3");
        StringBuilder vod_play_url = new StringBuilder();
        StringBuilder vod_play_from = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            String playFromText = circuits.get(i).text();
            vod_play_from.append(playFromText).append("$$$");
            Elements line = sources.get(i).select("a");
            for (int j = 0; j < line.size(); j++) {
                Element a = line.get(j);
                String href = a.attr("href");
                String text = a.text();
                vod_play_url.append(text).append("$").append(href);
                boolean notLastEpisode = j < line.size() - 1;
                vod_play_url.append(notLastEpisode ? "#" : "$$$");
            }
        }

        String title = doc.select(".stui-content__thumb a").attr("title");
        String pic = doc.select(".stui-content__thumb a > img").attr("data-original");
        String remarks = doc.select(".stui-content__detail p.data:nth-child(2)").text();
        String type = doc.select(".stui-content__detail p.data:nth-child(5)").text();
        String actor = doc.select(".stui-content__detail p.data:nth-child(3)").text();
        String director = doc.select(".stui-content__detail p.data:nth-child(4)").text();
        String brief = doc.select(".stui-content__desc").text();

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(title);
        vod.setVodPic(pic);
        vod.setTypeName(type);
        vod.setVodActor(actor);
        vod.setVodContent(brief);
        vod.setVodRemarks(remarks);
        vod.setVodDirector(director);
        vod.setVodPlayFrom(vod_play_from.toString());
        vod.setVodPlayUrl(vod_play_url.toString());
        return Result.string(vod);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String content = OkHttp.string(siteUrl + id, getHeader());
        Matcher matcher = Pattern.compile("player_aaaa=(.*?)</script>").matcher(content);
        String json = matcher.find() ? matcher.group(1) : "";
        JSONObject player = new JSONObject(json);
        String rawUrl = player.getString("url").replace("\\", "");
        if (rawUrl.contains(".m3u8") || rawUrl.contains(".mp4")) {
            return Result.get().url(rawUrl).header(getHeader()).string();
        }
        String from = player.getString("from");
        String data = "&data=" + player.getString("play_data");
        String staticUrl = siteUrl + "/static/player/" + from + ".js";
        String content1 = OkHttp.string(staticUrl, getHeader());
        Matcher matcher1 = Pattern.compile("src=\"(.*?)'").matcher(content1);
        String matchUrl = matcher1.find() ? matcher1.group(1) : "";
        String sechtml = OkHttp.string(matchUrl + rawUrl + data, getHeader());
        String realUrl = StringUtils.substringBetween(sechtml, "var urls = '", "'");
        return Result.get().url(realUrl).header(getHeader()).string();
        }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String searchUrl = siteUrl + "/index.php/ajax/suggest.html?mid=1&wd=" + URLEncoder.encode(key);
        String content = OkHttp.string(searchUrl, getHeader());
        JSONArray lists = new JSONObject(content).getJSONArray("list");
        List<Vod> list = new ArrayList<>();
        for (int i = 0; i < lists.length(); i++) {
            JSONObject item = lists.getJSONObject(i);
            String vid = siteUrl + "/detail/" + item.getInt("id") + ".html";
            String name = item.getString("name");
            String pic = item.getString("pic");
            list.add(new Vod(vid, name, pic));
        }
        return Result.string(list);
    }
}
