package com.github.tvbox.osc.server;

import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.orhanobut.hawk.Hawk;
import org.greenrobot.eventbus.EventBus;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author pj567
 * @date :2021/1/5
 * @description: 响应按键和输入
 */

public class InputRequestProcess implements RequestProcess {
    private RemoteServer remoteServer;

    public InputRequestProcess(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            switch (fileName) {
                case "/action":
                    return true;
            }
        }
        return false;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        switch (fileName) {
            case "/action":
                if (params.get("do") != null) {
                    String action = params.get("do");

                    switch (action) {
                        case "search": {
                            DataReceiver mDataReceiver = remoteServer.getDataReceiver();
                            if (mDataReceiver != null) {
                                mDataReceiver.onTextReceived(params.get("word").trim());
                            }
                            break;
                        }
                        case "api": {
                            // 获取并处理URL参数
                            String urlParam = params.get("url");
                            if (urlParam != null) {
                                String url = urlParam.trim();
                                // 发送直播接口更新事件
                                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_LIVE_API_URL_CHANGE, url));
                                // 保存到配置
                                Hawk.put(HawkConfig.LIVE_API_URL, url);
                                // 保存历史记录
                                HistoryHelper.setLiveApiHistory(url);
                            }
                            break;
                        }
                    }
                }
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");
            default:
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.");
        }
    }}
