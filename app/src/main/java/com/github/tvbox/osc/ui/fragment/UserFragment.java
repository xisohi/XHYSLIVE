package com.github.tvbox.osc.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.adapter.HomeHotVodAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.UA;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class UserFragment extends BaseLazyFragment implements View.OnClickListener {
    private LinearLayout tvLive;
    private LinearLayout tvSearch;
    private LinearLayout tvSetting;
    private LinearLayout tvHistory;
    private LinearLayout tvCollect;
    public static HomeHotVodAdapter homeHotVodAdapter;
    private List<Movie.Video> homeSourceRec;
    public static TvRecyclerView tvHotList;

    public static UserFragment newInstance() {
        return new UserFragment();
    }

    public UserFragment setArguments(List<Movie.Video> recVod) {
        this.homeSourceRec = recVod;
        return this;
    }

    @Override
    protected void onFragmentResume() {
        super.onFragmentResume();
        if (Hawk.get(HawkConfig.HOME_REC_STYLE, false)) {
            tvHotList.setVisibility(View.VISIBLE);
            tvHotList.setHasFixedSize(true);
            int spanCount = 5;
            if(style != null) spanCount = ImgUtil.spanCountByStyle(style, spanCount);
            tvHotList.setLayoutManager(new V7GridLayoutManager(this.mContext, spanCount));
            int paddingLeft = getResources().getDimensionPixelSize(R.dimen.vs_15);
            int paddingTop = getResources().getDimensionPixelSize(R.dimen.vs_10);
            int paddingRight = getResources().getDimensionPixelSize(R.dimen.vs_15);
            int paddingBottom = getResources().getDimensionPixelSize(R.dimen.vs_10);
            tvHotList.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        } else {
            tvHotList.setVisibility(View.VISIBLE);
            tvHotList.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.HORIZONTAL, false));
            int paddingLeft = getResources().getDimensionPixelSize(R.dimen.vs_15);
            int paddingTop = getResources().getDimensionPixelSize(R.dimen.vs_40);
            int paddingRight = getResources().getDimensionPixelSize(R.dimen.vs_15);
            int paddingBottom = getResources().getDimensionPixelSize(R.dimen.vs_40);
            tvHotList.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

        // 固定加载豆瓣热播数据
        setDouBanData(homeHotVodAdapter);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_user;
    }

    private void jumpSearch(Movie.Video vod){
        Intent newIntent = new Intent(mContext, SearchActivity.class);
        newIntent.putExtra("title", vod.name);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(newIntent);
    }

    private ImgUtil.Style style;

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        tvLive = findViewById(R.id.tvLive);
        tvSearch = findViewById(R.id.tvSearch);
        tvSetting = findViewById(R.id.tvSetting);
        tvCollect = findViewById(R.id.tvFavorite);
        tvHistory = findViewById(R.id.tvHistory);
        tvLive.setOnClickListener(this);
        tvSearch.setOnClickListener(this);
        tvSetting.setOnClickListener(this);
        tvHistory.setOnClickListener(this);
        tvCollect.setOnClickListener(this);
        tvLive.setOnFocusChangeListener(focusChangeListener);
        tvSearch.setOnFocusChangeListener(focusChangeListener);
        tvSetting.setOnFocusChangeListener(focusChangeListener);
        tvHistory.setOnFocusChangeListener(focusChangeListener);
        tvCollect.setOnFocusChangeListener(focusChangeListener);
        tvHotList = findViewById(R.id.tvHotList);

        // 创建适配器，固定显示豆瓣热播
        style = ImgUtil.initStyle();
        homeHotVodAdapter = new HomeHotVodAdapter(style, "豆瓣");
        homeHotVodAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (ApiConfig.get().getSourceBeanList().isEmpty())
                    return;
                Movie.Video vod = ((Movie.Video) adapter.getItem(position));

                // 移除历史记录删除模式的相关代码
                if (vod.id != null && !vod.id.isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", vod.id);
                    bundle.putString("sourceKey", vod.sourceKey);
                    SourceBean sourceBean = ApiConfig.get().getSource(vod.sourceKey);
                    if(sourceBean != null){
                        bundle.putString("picture", vod.pic);
                        jumpActivity(DetailActivity.class, bundle);
                    } else {
                        jumpSearch(vod);
                    }
                } else {
                    jumpSearch(vod);
                }
            }
        });

        homeHotVodAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                if (ApiConfig.get().getSourceBeanList().isEmpty()) return false;
                Movie.Video vod = ((Movie.Video) adapter.getItem(position));
                // 移除聚合搜索相关代码
                assert vod != null;
                if ((vod.id != null && !vod.id.isEmpty())) {
                    // 移除历史记录删除模式
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("title", vod.name);
                    // 改为跳转到普通搜索页面
                    jumpActivity(SearchActivity.class, bundle);
                }
                return true;
            }
        });

        tvHotList.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        tvHotList.setAdapter(homeHotVodAdapter);

        initHomeHotVod(homeHotVodAdapter);
    }

    private void initHomeHotVod(HomeHotVodAdapter adapter) {
        // 固定加载豆瓣热播数据
        setDouBanData(adapter);
    }

    private void setDouBanData(HomeHotVodAdapter adapter) {
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            String today = String.format("%d%d%d", year, month, day);
            String requestDay = Hawk.get("home_hot_day", "");
            if (requestDay.equals(today)) {
                String json = Hawk.get("home_hot", "");
                if (!json.isEmpty()) {
                    ArrayList<Movie.Video> hotMovies = loadHots(json);
                    if (hotMovies != null && hotMovies.size() > 0) {
                        adapter.setNewData(hotMovies);
                        return;
                    }
                }
            }
            String doubanUrl = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=" + year + "," + year;
            OkGo.<String>get(doubanUrl)
                    .headers("User-Agent", UA.randomOne())
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            String netJson = response.body();
                            Hawk.put("home_hot_day", today);
                            Hawk.put("home_hot", netJson);
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setNewData(loadHots(netJson));
                                }
                            });
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            return response.body().string();
                        }
                    });
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private ArrayList<Movie.Video> loadHots(String json) {
        ArrayList<Movie.Video> result = new ArrayList<>();
        try {
            JsonObject infoJson = new Gson().fromJson(json, JsonObject.class);
            JsonArray array = infoJson.getAsJsonArray("data");
            int limit = Math.min(array.size(), 25);
            for (int i = 0; i < limit; i++) {  // 改用索引循环
                JsonElement ele = array.get(i);
                JsonObject obj = ele.getAsJsonObject();
                Movie.Video vod = new Movie.Video();
                vod.name = obj.get("title").getAsString();
                vod.note = obj.get("rate").getAsString();
                if (!vod.note.isEmpty()) vod.note += " 分";
                vod.pic = obj.get("cover").getAsString()
                        + "@User-Agent=" + UA.randomOne()
                        + "@Referer=https://www.douban.com/";

                result.add(vod);
            }
        } catch (Throwable th) {

        }
        return result;
    }

    private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            else
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
        }
    };

    @Override
    public void onClick(View v) {
        // 清除长按删除模式
        HawkConfig.hotVodDelete = false;

        FastClickCheckUtil.check(v);
        if (v.getId() == R.id.tvLive) {
            if(Hawk.get(HawkConfig.LIVE_GROUP_LIST, new JsonArray()).isEmpty()){
                Toast.makeText(mContext, "直播源为空", Toast.LENGTH_SHORT).show();
            } else {
                jumpActivity(LivePlayActivity.class);
            }
        } else if (v.getId() == R.id.tvSearch) {
            jumpActivity(SearchActivity.class);
        } else if (v.getId() == R.id.tvSetting) {
            jumpActivity(SettingActivity.class);
        } else if (v.getId() == R.id.tvFavorite) {
            jumpActivity(CollectActivity.class);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_CONNECTION) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}