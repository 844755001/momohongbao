package com.liuchang.momohongbao.activity;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.liuchang.momohongbao.R;
import com.liuchang.momohongbao.adapter.HongbaoAdapter;
import com.liuchang.momohongbao.base.App;
import com.liuchang.momohongbao.base.BaseActivity;
import com.liuchang.momohongbao.model.bean.Hongbao;
import com.liuchang.momohongbao.model.db.DaoSession;
import com.liuchang.momohongbao.model.db.HongbaoDao;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;

public class HBListActivity extends BaseActivity {
    @BindView(R.id.current_day_money)
    TextView allMoney;
    @BindView(R.id.list)
    RecyclerView list;

    private HongbaoAdapter adapter;
    private HongbaoDao dao;

    @Override
    protected int initLayoutId() {
        return R.layout.activity_hb_list;
    }

    @Override
    protected void init() {
        DaoSession daoSession = App.getInstance().getDaoSession();
        dao = daoSession.getHongbaoDao();
        initView();
        initData();
    }

    private void initView() {
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HongbaoAdapter();
        list.setAdapter(adapter);
    }

    private void initData() {
        List<Hongbao> data = dao.queryBuilder().orderDesc(HongbaoDao.Properties.Id).build().list();
        adapter.refreshData(data);
        BigDecimal bd = new BigDecimal(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = sdf.format(new Date());
        for (Hongbao hongbao : data) {
            if (date.equals(hongbao.getDate())) {
                bd = bd.add(new BigDecimal(hongbao.getAmount()));
            }
        }
        allMoney.setText("今日收益：" + String.format("%.2f", bd.doubleValue()));
    }
}
