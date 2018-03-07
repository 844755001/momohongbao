package com.liuchang.momohongbao.base;

import android.app.Application;

import com.liuchang.momohongbao.model.db.DaoMaster;
import com.liuchang.momohongbao.model.db.DaoSession;

import org.greenrobot.greendao.database.Database;


public class App extends Application {

    private static App instance;
    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "hongbao");
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public static App getInstance() {
        return instance;
    }

}
