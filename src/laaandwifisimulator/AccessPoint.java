/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package laaandwifisimulator;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * WiFiAP, LTE BSに関する共通の情報
 *
 * @author ginnan
 */
public abstract class AccessPoint {

    protected LinkedList<UserNode> UserList; //接続しているユーザのリスト

    protected int connecting_num; //接続しているユーザ数

    protected int ap_id; //WiFi AP ID (0~9999) or LTE BS のID (10000~)

    protected int wifi_lteu_indicator; //WiFiか,LTE-Uかを示す⇒0:WiFi, 1:LTE-U

    protected double capacity; //提供可能な容量
    protected double max_capacity;//使っていない

    protected int located_area_id; //配置されている場所のID

    protected ArrayList<Integer> cover_area_list; //カバーしているエリアのリスト

    protected int assigned_channel; //割り当てられているチャネル

    protected ArrayList<Integer> overray_list; //重複しているAP,BSのリスト

    protected LinkedList<Integer> interference_list;    //干渉しているAP, BSのリスト

    protected double user_throughput; //ユーザが得ているスループット

    public AccessPoint(int ap_id) {
        UserList = new LinkedList<UserNode>();
        this.ap_id = ap_id;
        connecting_num = 0;
        cover_area_list = new ArrayList<>();
    }

    //到着したユーザの接続処理
    public void connect(UserNode user, double current_time, int service_set) {

        connecting_num++;

        //新規ユーザのスループットを計算
        double throughput = getThroughput();
        user.setAssignedThroughput(throughput);
        user_throughput = throughput;

        //接続中ユーザのスループットを再計算
        if (service_set == 0) {   //ファイルダウンロード
            Utility.ReCalcUserthroughput(UserList, throughput, current_time);
        } else {  //一定時間通信
            Utility.ReCalcUserthroughput2(UserList, throughput, current_time);
        }

        //AP,BSのユーザリストに格納
        UserList.add(user);
    }

    //ユーザの再接続の処理
    public void reconnect(double current_time, int service_set) {
        double throughput = getThroughput();
        user_throughput = throughput;
        //接続中ユーザのスループットを再計算
        if (service_set == 0) {   //ファイルダウンロード
            Utility.ReCalcUserthroughput(UserList, throughput, current_time);
        } else {  //一定時間通信
            Utility.ReCalcUserthroughput2(UserList, throughput, current_time);
        }
    }

    //ユーザ一人あたりのスループットを計算
    private double getThroughput() {
        return Utility.CalcUserthroughput(capacity, connecting_num);
    }

    /*通信の終了の処理*/
    public void commEndProcess(UserNode user, double current_time, int service_set) {

        connecting_num--;
        UserList.remove(user);
        double thr = getThroughput();
        user_throughput = thr;

        if (service_set == 0) {
            Utility.ReCalcUserthroughput(UserList, thr, current_time);
        } else {
            Utility.ReCalcUserthroughput2(UserList, thr, current_time);
        }
    }
}
