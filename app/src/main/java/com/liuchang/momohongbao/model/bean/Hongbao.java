package com.liuchang.momohongbao.model.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by liuchang on 2018/3/4.
 */
@Entity
public class Hongbao {
    @Id(autoincrement = true)
    private Long id;
    private String date;
    private String time;
    private double amount;

    @Generated(hash = 1001700666)
    public Hongbao(Long id, String date, String time, double amount) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.amount = amount;
    }

    public Hongbao(String date, String time, double amount) {
        this.date = date;
        this.time = time;
        this.amount = amount;
    }

    @Generated(hash = 2056618141)
    public Hongbao() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
