/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package laaandwifisimulator;

import java.io.IOException;
import java.util.LinkedList;

/**
 * WiFiとLTE-Uの混在環境におけるチャネル割り当て,接続先選択シミュレータ
 *
 * @author ginnan
 */
public class LAAandWiFiSimulator {

    /**
     * @param args the command line arguments
     */
    /*
     * 引数リスト
     * 0: loop_num -> ループ回数(シミュレーション回数)
     * 1: interval_time -> 周波数割り当て間隔(提案手法の実行間隔)
     * 2: wifi_user_lambda -> WiFiのみユーザの到着率
     * 3: lteu_user_lambda -> WiFi + LTE-Uユーザの到着率
     * 4: end_condition -> シミュレーション終了条件の選択(0:呼数, 1:時間)
     * 5: end_num -> 終了条件となる呼数 or 時間
     * 6: service_set -> ユーザのサービス利用形態 (0:ファイルダウンロード, 1:一定時間の通信)
     * 7: select_method -> 提案手法など、使う手法の選択
     * 8:GAのループ数
     * 9:突然変異の発生確率
     * 10:GAの個体数
     * 11:交叉時の親の組数
     * 12:エリート選択で選択する数
     * 
     */
    public static UserParameter _param;

    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        long start = System.currentTimeMillis();// 時間計測
        // 引数の代入
        String[] args1 = {
                "1", // loop_num -> 0: Count of for
                "300", // interval_time -> 1:Frequency allocation interval (GA execution interval)
                "0.0005", // wifi_user_lambda -> 2:WiFi only user arrival rate
                "0.0005", // lteu_user_lambda -> 3: Arrival rate of WiFi + LTE-U users
                "0", // end_condition -> 4: Selection of simulation end condition (0: number of
                     // calls, 1: time)
                "650", // end_num -> 5: Number of calls or time that is the end condition
                "1", // service_set -> 6: User usage (0: file download, 1: fixed time communication)
                "30", // select_method -> 7: Selection of proposed method, etc.
                "1000", // ga_loop_num -> 8: GA loop count
                "5", // mutation_prob -> 9: Mutation probability
                "16", // ga_individual_num -> 10: GA population
                "3", // crossover_parent_num -> 11: Number of pairs of parents at the time of
                     // crossover
                "1" // elite_num -> 12: Number to select in elite selection
        };
        _param = new UserParameter(args1);

        // シミュレーション結果をファイル出力するクラスの作成
        Output output = new Output(_param);

        System.out.println("Capacities installing.... ");

        // チャネル共有時の容量をセット
        Constants.CAPACITY_WITH_LAA_WIFI = Utility.SetCapacitySharedWiFiLTEU();
        Constants.CAPACITY_WITH_WIFIS = Utility.SetCapacitySharedWiFi();

        System.out.println("Topology installing....");

        // エリア、AP,BSのカバー範囲, LTE-Uの配置場所を取得
        AreaTopology Topology = new AreaTopology();

        // シナリオの作成
        Scenario scenario;

        Constants.SERVICE_SET = _param.service_set;

        Constants.are = args1[2];

        for (int i = 0; i < _param.loop_num; i++) {
            /* for文でループする場合 */
            System.out.println("Loop " + (i + 1));
            scenario = new Scenario(i + 30, _param, Topology, null);
            /* */

            /* 一回ごとの結果をだす場合 */
            // System.out.println("Loop " + _param.loop_num);
            // scenario = new Scenario((40+_param.loop_num), _param, Topology);
            /* */
            scenario.startSimulation();
            output.update(scenario);

            // シミュレーション中の全ユーザのスループットを記録したやつ→累積分布を出すのに用いる
            // if (_param.loop_num == 1) {
            // output.writeToFile_Throughput(scenario.getData().wifi_users_throghput,
            // scenario.getData().lteu_users_throghput,
            // scenario.getData().wifi_users_min_throghput,
            // scenario.getData().lteu_users_min_throghput);
            //
            // output.writeToFile_Throughput_time_Avg(scenario.getData().wifi_users_min_time_throghput,
            // scenario.getData().lteu_users_min_time_throghput);
            // }
        } // for_loop(一回ごとにシミュレーションする場合は消す)******************
        output.executeSimEnd();

        // シミュレーション結果をファイルに書き込む and 画面出力
        if (Constants.SERVICE_SET == 0) {// ファイルダウンロードの場合
            output.writeToFile();
            output.printToScreen();
        } else {// 一定時間通信の場合
            output.writeToFile2();
            output.printToScreen2();

        }

        // 時間計測
        long end = System.currentTimeMillis();
        System.out.println("Simulation Time: " + (end - start) + "[ms]");
        output.writeToFile_SimuTime(end - start);

    }

}
