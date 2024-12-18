package org.qpython.qpy.main.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.quseit.util.DateTimeHelper;
import com.quseit.util.ImageUtil;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;

import org.qpython.qpy.R;
import org.qpython.qpy.codeshare.CONSTANT;
import org.qpython.qpy.codeshare.ShareCodeUtil;
import org.qpython.qpy.codeshare.pojo.CloudFile;
import org.qpython.qpy.databinding.FragmentRefreshRvBinding;
import org.qpython.qpy.main.adapter.CloudScriptAdapter;
import org.qpython.qpy.texteditor.TedLocalActivity;
import org.qpython.qpysdk.QPyConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class MyProjectFragment extends Fragment {
    private int WIDTH = (int) ImageUtil.dp2px(60);
    private FragmentRefreshRvBinding binding;
    private CloudScriptAdapter adapter;
    private List<CloudFile> scriptList = new ArrayList<>();
    private TedLocalActivity activity;

    public boolean isLoading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.bind(LayoutInflater.from(getContext()).inflate(R.layout.fragment_refresh_rv, null));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scriptList = new ArrayList<>();
        adapter = new CloudScriptAdapter(scriptList);
        ShareCodeUtil.getInstance().clearAll();
        initView();
        retry(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (TedLocalActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//        activity.locatedCloud(scriptList);
    }

    public void retry(boolean forceRefresh) {
        if (scriptList != null && adapter != null) {
            scriptList.clear();
            adapter.notifyDataSetChanged();
        }
        startProgressBar();
        isLoading = true;
    }

    public void notifyDataSetChange() {
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void startProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.netError.setVisibility(View.GONE);
        Observable.just(null)
                .delay(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(o -> showNetErrorText())
                .subscribe();
    }

    private void showEmpty() {
        if (binding.progressBar.getVisibility() == View.VISIBLE) {
            binding.progressBar.setVisibility(View.GONE);
            binding.netError.setText(R.string.cloud_empty_hint);
            binding.netError.setVisibility(View.VISIBLE);
        }
    }

    private void showNetErrorText() {
        if (binding.progressBar.getVisibility() == View.VISIBLE) {
            binding.progressBar.setVisibility(View.GONE);
            binding.netError.setText(R.string.net_lagging);
            binding.netError.setVisibility(View.VISIBLE);
        }
    }

    private void initView() {
        SwipeMenuCreator swipeMenuCreator = (leftMenu, rightMenu, viewType) -> {
            SwipeMenuItem deleteItem = new SwipeMenuItem(getContext())
                    .setBackgroundColor(Color.parseColor("#FFD14136"))
                    .setImage(R.drawable.ic_editor_filetree_close)
                    .setHeight(ViewGroup.LayoutParams.MATCH_PARENT)
                    .setWidth(WIDTH);

            SwipeMenuItem downloadItem = new SwipeMenuItem(getContext())
                    .setBackgroundColor(Color.parseColor("#FF4798F3"))
                    .setImage(R.drawable.ic_cloud_download)
                    .setHeight(ViewGroup.LayoutParams.MATCH_PARENT)
                    .setWidth(WIDTH);
            rightMenu.addMenuItem(downloadItem);
            rightMenu.addMenuItem(deleteItem);
        };
        binding.swipeList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.swipeList.setSwipeMenuCreator(swipeMenuCreator);
    }
}
