/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package laaandwifisimulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 提案手法 : GAによるチャネル割り当て
 *
 * @author ginnan
 */
public class EventOfGenericAlgorithm extends Event {

    private final int GA_num = Constants.GA_NUM; //GAを回す回数(最大世代数)

    private int crossover_num;  //交叉をする回数(M回)

    private int[][] Parent;               // 個体の集団から、親のインデックスを格納する ([m組目の親][ 0 or 1 (親は二人)])

    private LinkedList<int[][]> individuals;  //[][0:BS,APのID, 1:割り当てチャネル]
    private ArrayList<Double> individuals_evaluations; //各個体の評価値を記入
    private ArrayList<Integer> evaluations_ids;  //評価値のうち、最小スループットを示しているIDを保存
    private int individuals_num;    //個体の数

    private final WiFiAP _wifi_ap[];
    private final LTEUBS _lteu_bs[];
    private static final int AREA_NUM = Constants.AREA_NUM;
    private static final int GA_NUM = Constants.GA_NUM;
    private static final int INDIVIDUAL_NUM = Constants.GA_INDIVIDUAL_NUM;
    private int[][] max_evaluate_individual;

    Parents[] parents = new Parents[Constants.PARENT_NUM];
    Parents[] childrens = new Parents[Constants.PARENT_NUM];

    CheckInterference ci;
    Random rn;

    int wifi_num; //LTE+WiFiユーザのうち、WiFiにつないでいたユーザ数
    int lte_num; //LTE+WiFiユーザのうち、LTE-Uにつないでいたユーザ数

    public EventOfGenericAlgorithm(double time, Scenario scenario) {
        super(scenario);
        this.event_time = time;
        _wifi_ap = _area.getWiFiAP();
        _lteu_bs = _area.getLTEUBS();
        individuals = new LinkedList<>();
        individuals_evaluations = new ArrayList<>();
        evaluations_ids = new ArrayList<>();

        individuals_num = Constants.GA_INDIVIDUAL_NUM;

        this.crossover_num = Constants.CROSSOVER_NUM;

        this.Parent = new int[Constants.PARENT_NUM][2]; //[親の組の数][親]

        parents = new Parents[Constants.PARENT_NUM];
        childrens = new Parents[Constants.PARENT_NUM];

        for (int i = 0; i < Constants.PARENT_NUM; i++) {
            parents[i] = new Parents(i);
            childrens[i] = new Parents(i);
        }

        ci = new CheckInterference(_area);
        rn = new Random();

        wifi_num = 0;
        lte_num = 0;
    }

    @Override
    public void runEvent() throws IOException {
        StartAlgorithm();

    }

    private void StartAlgorithm() throws IOException {
//        double debug_value = 99999;//デバッグ用の変数

        //デバッグ*****************
//        double th_p = 999999;
//        for (int i = 0; i < Constants.LTEU_NUM; i++) {
////            if(_wifi_ap[i].assigned_channel == -1) System.out.println(i + "\t"+_wifi_ap[i].located_area_id + "\t" + _wifi_ap[i].assigned_channel + "\t" + _wifi_ap[i].interference_list.size() + "\t" +_wifi_ap[i].UserList.size());
////            if(_lteu_bs[i].connecting_num == 0) 
////            System.out.println(i + "\t" + _lteu_bs[i].connecting_num + "\t" + _lteu_bs[i].capacity + "\t" + _lteu_bs[i].user_throughput);
//            if (_lteu_bs[i].user_throughput != 0 && th_p > _lteu_bs[i].user_throughput) {
//                th_p = _lteu_bs[i].user_throughput;
//            }
//        }
//        System.out.println(th_p + "***********************preevious");
//        th_p = 999999;
//        for (int i = 0; i < Constants.WiFi_NUM; i++) {
////            System.out.println(i + "\t" + _wifi_ap[i].connecting_num + "\t" + _wifi_ap[i].capacity + "\t" + _wifi_ap[i].user_throughput+ "\t" + _wifi_ap[i].located_area_id);
//            if (_wifi_ap[i].user_throughput != 0 && th_p > _wifi_ap[i].user_throughput) {
//                th_p = _wifi_ap[i].user_throughput;
//            }
//        }
//        System.out.println(th_p + "-------------------------previous");
        //***********************
        //ここまでで、今までの統計を取るための処理
        FirstReConnect();

        int[][] temp_individual;
        /* 初期集団の生成 */
//        temp_individual = CreateFirstIndividualsNeighborAssign();
//        individuals.add(temp_individual);
        for (int a = 0; a < individuals_num; a++) {
            temp_individual = CreateFirstIndividuals();
//          temp_individual = CreateFirstIndividualsChannelOn();
//          temp_individual = CreateFirstIndividualsNeighborAssign();
            individuals.add(temp_individual);
        }

        /*初期集団の評価 */
        for (int a = 0; a < individuals.size(); a++) {
            temp_individual = individuals.get(a);
            individuals_evaluations.add(Evaluate(temp_individual));
        }
//        System.out.println("Naze da?");

        //GA_num回だけGAを実行
        for (int x = 0; x < GA_num; x++) {

//            if (judgeMutation()) {
//                /*突然変異 */
//                Mutation();
//
//            }
            if (judgeMutation2()) {//System.out.println(x);
                /*突然変異 */
                Mutation2();

            }

            //デバッグ用
//            int[][] individual_i;
//            for (int i = 0; i < individuals.size(); i++) {
//                System.out.print(i + "|");
//                for (int a = Constants.LTEU_NUM; a < Constants.LTEU_NUM + Constants.WiFi_NUM; a++) {
//                    individual_i = individuals.get(i);
//                    System.out.print(individual_i[a][1] + " ");
//                }
//                System.out.println();
//            }
            //

            /*交叉 */
            for (int a = 0; a < crossover_num; a++) {
                Crossover();
            }

            /* 評価 */
            individuals_evaluations.clear();
            evaluations_ids.clear();
            for (int i = 0; i < individuals.size(); i++) {
                individuals_evaluations.add(Evaluate(individuals.get(i)));
            }

//            if(x == GA_num -1){
////                if(debug_value > individuals_evaluations.get(0)){
////                debug_value = individuals_evaluations.get(0);
////                }
//                System.out.println(x + "\t" + individuals_evaluations.get(0) +"\t"+ "evalu");
////                System.out.println("*");
//            }
            /* 淘汰 */
            Select();

            /* 最大の評価値の個体を保持しておく */
            max_evaluate_individual = individuals.peek();
        }

        //最大の評価値の個体をBS,APに適用し、接続先を変更
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].assigned_channel = max_evaluate_individual[i][1];
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = max_evaluate_individual[i + Constants.LTEU_NUM][1];
        }

        //干渉の容量をセット
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].interference_list = ci.LTEUCheck(i, _wifi_ap, _lteu_bs);
            _lteu_bs[i].SetCapacity();
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i, _wifi_ap, _lteu_bs);
            _wifi_ap[i].SetCapacity();

        }

        //ここで、ユーザの接続先選択をする
        ProposedReConnect(max_evaluate_individual);
//        ReConnectNotChange(max_evaluate_individual);

        _area.CopyLTEUBS(_lteu_bs);
        _area.CopyWiFiAP(_wifi_ap);

        //デバッグ*****************
//        double th =999999;
//        for (int i = 0; i < Constants.LTEU_NUM; i++) {
////            if(_wifi_ap[i].assigned_channel == -1) System.out.println(i + "\t"+_wifi_ap[i].located_area_id + "\t" + _wifi_ap[i].assigned_channel + "\t" + _wifi_ap[i].interference_list.size() + "\t" +_wifi_ap[i].UserList.size());
////            if(_lteu_bs[i].connecting_num == 0) 
//                System.out.println( i +"\t"+ _lteu_bs[i].connecting_num +"\t"+_lteu_bs[i].capacity+"\t"+_lteu_bs[i].user_throughput);
//            if(_lteu_bs[i].user_throughput != 0 &&th > _lteu_bs[i].user_throughput) th = _lteu_bs[i].user_throughput;
//        }
//         System.out.println(th + "*****");
//***********************
        //デバッグ*****************
//        double th = 99999;
//        for (int i = 0; i < Constants.LTEU_NUM; i++) {
////            if(_wifi_ap[i].assigned_channel == -1) System.out.println(i + "\t"+_wifi_ap[i].located_area_id + "\t" + _wifi_ap[i].assigned_channel + "\t" + _wifi_ap[i].interference_list.size() + "\t" +_wifi_ap[i].UserList.size());
////            if(_lteu_bs[i].connecting_num == 0) 
////            System.out.println(i + "\t" + _lteu_bs[i].connecting_num + "\t" + _lteu_bs[i].capacity + "\t" + _lteu_bs[i].user_throughput);
//            if (_lteu_bs[i].user_throughput != 0 && th > _lteu_bs[i].user_throughput) {
//                th = _lteu_bs[i].user_throughput;
//            }
//        }
//        System.out.println(th + "***********************");
//        th = 99999;
//        for (int i = 0; i < Constants.WiFi_NUM; i++) {
////            System.out.println(i + "\t" + _wifi_ap[i].connecting_num + "\t" + _wifi_ap[i].capacity + "\t" + _wifi_ap[i].user_throughput+ "\t" + _wifi_ap[i].located_area_id);
//            if (_wifi_ap[i].user_throughput != 0 && th > _wifi_ap[i].user_throughput) {
//                th = _wifi_ap[i].user_throughput;
//            }
//        }
//        System.out.println(th + "-------------------------");
//        if(th_p > th){ System.out.println(th_p + "\t" + th);}
        //***********************
        /*最小スループットの時間平均の処理*/
        _scenario.gettimeMinData(event_time);

        /*次の提案手法の発生イベントを作る*/
        double time = _queue.getCurrentTime() + _param.interval_time;
        Event next_event = new EventOfGenericAlgorithm(time, _scenario);
        _queue.add(next_event);

    }

    /* 初期集団の個体を生成：ランダム割り当て */
    private int[][] CreateFirstIndividuals() {

        int[][] temp_individual = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            temp_individual[i][0] = _lteu_bs[i].ap_id;
            temp_individual[i][1] = _lteu_bs[i].assigned_channel;//LTE-Uはチャネルを変えない   //rn.nextInt(Constants.CHANNEL_NUM + 1) - 1; //-1の場合、チャネルを割り当てない
        }

        do {
            for (int i = 0; i < Constants.WiFi_NUM; i++) {
                temp_individual[i + Constants.LTEU_NUM][0] = _wifi_ap[i].ap_id;
                temp_individual[i + Constants.LTEU_NUM][1] = rn.nextInt(Constants.CHANNEL_NUM + 1) - 1; //-1の場合、チャネルを割り当てない
            }
        } while (!judgeConstraintOne(temp_individual));

        //制約条件を満たしているかを確認
        return temp_individual;

    }

    /* 初期集団の個体を生成：チャネルすべてonでランダム割り当て */
    private int[][] CreateFirstIndividualsChannelOn() {

        int[][] temp_individual = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            temp_individual[i][0] = _lteu_bs[i].ap_id;
            temp_individual[i][1] = _lteu_bs[i].assigned_channel;//LTE-Uはチャネルを変えない   //rn.nextInt(Constants.CHANNEL_NUM + 1) - 1; //-1の場合、チャネルを割り当てない
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            temp_individual[i + Constants.LTEU_NUM][0] = _wifi_ap[i].ap_id;
            temp_individual[i + Constants.LTEU_NUM][1] = rn.nextInt(Constants.CHANNEL_NUM);
        }

        //制約条件を満たしているかを確認
        return temp_individual;

    }

    /* 初期集団の個体を生成：ランダム割り当て後,可能な限りチャネルoff*/
    private int[][] CreateFirstIndividualsChannelOff() {
        int[][] temp_individual = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
        int area_id;
        int[] channel_overray_check;    //1つのWiFi APに対して、カバーしているエリアごとの重複チャネル数をチェック

        temp_individual = CreateFirstIndividuals();
        //一旦、個体のチャネル割り当てをAPに適用
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = temp_individual[i + Constants.LTEU_NUM][1];
        }

        ArrayList<Integer> cover_areas;
        ArrayList<Integer>[] area_covered_aps = _area.getAreaCooveredAPSet();
        boolean not_change = false;
        //WiFi AP IDを0から見ていって、可能な限りoffにする
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            cover_areas = _wifi_ap[i].cover_area_list;
            //カバーエリアを見て、APのチャネルをoffにできるかを判断
            channel_overray_check = new int[cover_areas.size()];
            for (int j = 0; j < cover_areas.size(); j++) {//WiFiのカバーエリアの探索
                area_id = cover_areas.get(j);
                for (int k = 0; k < area_covered_aps[area_id].size(); k++) { //最小エリアごとのAPの探索
                    if (_wifi_ap[area_covered_aps[area_id].get(k)].assigned_channel >= 0) {
                        channel_overray_check[j]++;
                    }
                }
            }
            //channel_overray_checkから、すべてのエリアを確認
            for (int j = 0; j < cover_areas.size(); j++) {
                if (channel_overray_check[j] == 0) {
                    not_change = true;
                    break;//そのAPではチャネルoffにできない。
                }
            }

            if (!not_change) {
                _wifi_ap[i].assigned_channel = -1;
            }
        }

        //突然変異の個体を保存
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            temp_individual[i + Constants.LTEU_NUM][1] = _wifi_ap[i].assigned_channel;
        }

        return temp_individual;
    }

    /* 初期集団の個体を生成：隣接チャネルを見て、干渉が少なくなるような割り当て*/
    private int[][] CreateFirstIndividualsNeighborAssign() {
        int[][] temp_individual = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];

        //最初はチャネル未割当に
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = -1;
        }

//        for(int j = 0; j < Constants.LTEU_NUM; j++){
//            _lteu_bs[j].assigned_channel = -1;
//        }
        //
        //チャネル割り当ての実行
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = ci.WiFiLeastInterference(i, _wifi_ap, _lteu_bs);
        }

        //干渉と容量のセット
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i);
            _wifi_ap[i].SetCapacity();
        }

        for (int j = 0; j < Constants.LTEU_NUM; j++) {
            _lteu_bs[j].interference_list = ci.LTEUCheck(j);
            _lteu_bs[j].SetCapacity();
        }

        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            temp_individual[i][0] = _lteu_bs[i].ap_id;
            temp_individual[i][1] = _lteu_bs[i].assigned_channel;//LTE-Uはチャネルを変えない   //rn.nextInt(Constants.CHANNEL_NUM + 1) - 1; //-1の場合、チャネルを割り当てない
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            temp_individual[i + Constants.LTEU_NUM][0] = _wifi_ap[i].ap_id;
            temp_individual[i + Constants.LTEU_NUM][1] = _wifi_ap[i].assigned_channel;
        }

        return temp_individual;
    }

    /* 個体の制約条件を確認 :WiFiのカバーエリアがなくならない*/
    private boolean judgeConstraintOne(int[][] individual) {

        //一時的にAPのチャネル変更(LTE-U BSは交叉の対象ではないので、変更しない)
        int covered_channel;
        ArrayList<Integer>[] ap_cover_area = _area.getAreaCooveredAPSet();

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = individual[i + Constants.LTEU_NUM][1];
        }
        for (int a = 0; a < Constants.AREA_NUM; a++) {
            covered_channel = -1;
            for (int b = 0; b < ap_cover_area[a].size(); b++) {
                if (_wifi_ap[ap_cover_area[a].get(b)].assigned_channel != -1) {
                    covered_channel = _wifi_ap[ap_cover_area[a].get(b)].assigned_channel;
                }
            }

            if (covered_channel == -1) {//制約条件を満たさない
                return false;
            }
        }
        return true;
    }

    /* 交叉 */
    private void Crossover() {

        //1.Constants.PARENT_NUM組の親個体を2つ選択
        for (int j = 0; j < Constants.PARENT_NUM; j++) {
            Parent[j][0] = rn.nextInt(individuals.size());

            do {
                Parent[j][1] = rn.nextInt(individuals.size());
            } while (Parent[j][0] == Parent[j][1]);

            parents[j].Parent1 = individuals.get(Parent[j][0]);
            parents[j].Parent2 = individuals.get(Parent[j][1]);

        }

        //2.交叉方法を選択して交叉
        for (int j = 0; j < Constants.PARENT_NUM; j++) {
//            DoCrossoverPerArea(j);
            DoCrossoverMostInterferenceAP(j);
        }

    }

    /* エリア単位の交叉 */
    private void DoCrossoverPerArea(int j) {
        boolean judge_selected = false;
        int selected_ap = -1;
        int[][] temp_individual_1;
        int[][] temp_individual_2;
        int[] overray_ap_list;
        int index;
        boolean judge_constraint = false;
        ArrayList<Integer> check_selected_aps = new ArrayList<>();
        boolean failure = false;
        Parents temp_parents;

        while (!judge_constraint) {
            //1.親個体を一時てきに保持 (Parentsを取得ごとに)
            temp_individual_1 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
            temp_individual_2 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
            temp_parents = new Parents(parents[j]);
            for (int i = 0; i < temp_parents.Parent1.length; i++) {
                System.arraycopy(temp_parents.Parent1[i], 0, temp_individual_1[i], 0, temp_parents.Parent1[i].length);
            }

            for (int i = 0; i < temp_parents.Parent2.length; i++) {
                System.arraycopy(temp_parents.Parent2[i], 0, temp_individual_2[i], 0, temp_parents.Parent2[i].length);
            }

//            temp_individual_1 = temp_parents.Parent1;
//            temp_individual_2 = temp_parents.Parent2;
            //2.交叉対象となるWiFi APを決定(個体でのID)
            while (!judge_selected) {
                selected_ap = rn.nextInt(Constants.WiFi_NUM) + Constants.LTEU_NUM; 
                judge_selected = true;

                for (int i = 0; i < check_selected_aps.size(); i++) {
                    if (check_selected_aps.get(i) == selected_ap) {
                        judge_selected = false;
                        break;
                    }
                }
            }

            //3.選択したAPの重複APリストを取得
            index = _wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.size();
            overray_ap_list = new int[index];
            for (int a = 0; a < index; a++) {
                if (_wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.get(a) < 10000) { //LTE-U BSは交叉の対象外
                    overray_ap_list[a] = _wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.get(a);
                }
            }

            //4.制約条件の確認⇒だめならAP選択し直し
            judge_constraint = JudgeConstraint(selected_ap, overray_ap_list, temp_individual_1, temp_individual_2);

            //5. 交叉ができなかった場合のAPを確認⇒すべてのAPでやってダメだったら
            check_selected_aps.add(selected_ap);
            if (check_selected_aps.size() == Constants.WiFi_NUM) {
                failure = true;
                break;
            }
        }

        //交叉の実行
        if (!failure) {
            int temp;

            temp_individual_1 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
            temp_individual_2 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
            temp_parents = new Parents(parents[j]);
            for (int i = 0; i < temp_parents.Parent1.length; i++) {
                System.arraycopy(temp_parents.Parent1[i], 0, temp_individual_1[i], 0, temp_parents.Parent1[i].length);
            }

            for (int i = 0; i < temp_parents.Parent2.length; i++) {
                System.arraycopy(temp_parents.Parent2[i], 0, temp_individual_2[i], 0, temp_parents.Parent2[i].length);
            }

            index = _wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.size();
            overray_ap_list = new int[index];
            for (int a = 0; a < index; a++) {
                if (_wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.get(a) < 10000) { //LTE-U BSは交叉の対象外
                    overray_ap_list[a] = _wifi_ap[selected_ap - Constants.LTEU_NUM].overray_list.get(a);
                }
            }

            temp = temp_individual_1[selected_ap][1];
            temp_individual_1[selected_ap][1] = temp_individual_2[selected_ap][1];
            temp_individual_2[selected_ap][1] = temp;

            for (int i = 0; i < overray_ap_list.length; i++) { //※※今の実装ならindividualとWiFiAPのIDは以下のようにして一致できるはず※※
                temp = temp_individual_1[overray_ap_list[i] + Constants.LTEU_NUM][1];
                temp_individual_1[overray_ap_list[i] + Constants.LTEU_NUM][1] = temp_individual_2[overray_ap_list[i] + Constants.LTEU_NUM][1];
                temp_individual_2[overray_ap_list[i] + Constants.LTEU_NUM][1] = temp;
            }

            //子となる個体を保存
            childrens[j].Parent1 = temp_individual_1;
            childrens[j].Parent2 = temp_individual_2;
            individuals.add(childrens[j].Parent1);
            individuals_evaluations.add(Evaluate(childrens[j].Parent1));
            individuals.add(childrens[j].Parent2);
            individuals_evaluations.add(Evaluate(childrens[j].Parent2));
        }

    }

    /* 交叉時の制約条件の判定 : WiFi APのカバーエリアがなくならない */
    private boolean JudgeConstraint(int selected_ap, int[] overray_ap_list, int[][] temp_individual_1, int[][] temp_individual_2) {
        int temp;

        //一時的に交叉をする
        temp = temp_individual_1[selected_ap][1];
        temp_individual_1[selected_ap][1] = temp_individual_2[selected_ap][1];
        temp_individual_2[selected_ap][1] = temp;

        for (int i = 0; i < overray_ap_list.length; i++) { //※※今の実装ならindividualとWiFiAPのIDは以下のようにして一致できるはず※※
            temp = temp_individual_1[overray_ap_list[i] + Constants.LTEU_NUM][1];
            temp_individual_1[overray_ap_list[i] + Constants.LTEU_NUM][1] = temp_individual_2[overray_ap_list[i] + Constants.LTEU_NUM][1];
            temp_individual_2[overray_ap_list[i] + Constants.LTEU_NUM][1] = temp;
        }

        //一時的にAPのチャネル変更(LTE-U BSは交叉の対象ではないので、変更しない)
        int covered_channel;
        ArrayList<Integer>[] ap_cover_area = _area.getAreaCooveredAPSet();

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = temp_individual_1[i + Constants.LTEU_NUM][1];
        }
        for (int a = 0; a < Constants.AREA_NUM; a++) {
            covered_channel = -1;
            for (int b = 0; b < ap_cover_area[a].size(); b++) {
                if (_wifi_ap[ap_cover_area[a].get(b)].assigned_channel != -1) {
                    covered_channel = _wifi_ap[ap_cover_area[a].get(b)].assigned_channel;
                }
            }

            if (covered_channel == -1) {//交叉失敗
                return false;
            }
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = temp_individual_2[i + Constants.LTEU_NUM][1];
        }
        for (int a = 0; a < Constants.AREA_NUM; a++) {
            covered_channel = -1;
            for (int b = 0; b < ap_cover_area[a].size(); b++) {
                if (_wifi_ap[ap_cover_area[a].get(b)].assigned_channel != -1) {
                    covered_channel = _wifi_ap[ap_cover_area[a].get(b)].assigned_channel;
                }
            }

            if (covered_channel == -1) {//交叉失敗
                return false;
            }
        }

        return true;
    }

    /* 干渉が多いAPを交叉 :change_num個のAPを交叉*/
    private void DoCrossoverMostInterferenceAP(int j) {
        int[][] temp_individual_1;
        int[][] temp_individual_2;
        boolean judge_constraint = false;
        boolean judge_constraint2 = false;
        Parents temp_parents;
        int change_num = 5;
        int temp;

        temp_individual_1 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
        temp_individual_2 = new int[Constants.LTEU_NUM + Constants.WiFi_NUM][2];
        temp_parents = new Parents(parents[j]);
        for (int i = 0; i < temp_parents.Parent1.length; i++) {
            System.arraycopy(temp_parents.Parent1[i], 0, temp_individual_1[i], 0, temp_parents.Parent1[i].length);
        }

        for (int i = 0; i < temp_parents.Parent2.length; i++) {
            System.arraycopy(temp_parents.Parent2[i], 0, temp_individual_2[i], 0, temp_parents.Parent2[i].length);
        }

        ArrayList<Integer> change_ap_list = new ArrayList<>();
        InterferenceOrder io;
        PriorityQueue<InterferenceOrder> inter_pq = new PriorityQueue<>(new MyComparator2());

        //temp_individual_1で干渉が多いAPを選択----------------------------------------------
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].assigned_channel = temp_individual_1[i][1];
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = temp_individual_1[i + Constants.LTEU_NUM][1];
        }

        //干渉のセット
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].interference_list = ci.LTEUCheck(i, _wifi_ap, _lteu_bs);
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i, _wifi_ap, _lteu_bs);
        }

        for (int a = Constants.LTEU_NUM; a < Constants.LTEU_NUM + Constants.WiFi_NUM; a++) {
            io = new InterferenceOrder(_wifi_ap[a - Constants.LTEU_NUM].interference_list.size(), a);
            inter_pq.add(io);
        }

        for (int x = 0; x < change_num; x++) {
            change_ap_list.add(inter_pq.poll().individual_id);
        }

        inter_pq.clear();
        //temp_individual_1での処理終了---------------------------------------------------------
        //temp_individual_2で干渉が多いAPを選択----------------------------------------------
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].assigned_channel = temp_individual_2[i][1];
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = temp_individual_2[i + Constants.LTEU_NUM][1];
        }

        //干渉のセット
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].interference_list = ci.LTEUCheck(i, _wifi_ap, _lteu_bs);
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i, _wifi_ap, _lteu_bs);
        }

        for (int a = Constants.LTEU_NUM; a < Constants.LTEU_NUM + Constants.WiFi_NUM; a++) {
            io = new InterferenceOrder(_wifi_ap[a - Constants.LTEU_NUM].interference_list.size(), a);
            inter_pq.add(io);
        }

        int id;
        int flag;
        for (int x = 0; x < change_num; x++) {
            flag = 0;
            id = inter_pq.poll().individual_id;
            for (int y = 0; y < change_num; y++) {
                if (change_ap_list.get(y) == id) {
                    flag = 1;
                }
            }
            if (flag == 0) {
                change_ap_list.add(id);
            }

        }

        inter_pq.clear();
        //temp_individual_2での処理終了---------------------------------------------------------

        for (int i = 0; i < change_ap_list.size(); i++) {
            temp = temp_individual_1[change_ap_list.get(i)][1];
            temp_individual_1[change_ap_list.get(i)][1] = temp_individual_2[change_ap_list.get(i)][1];
            temp_individual_2[change_ap_list.get(i)][1] = temp;

            judge_constraint = judgeConstraintOne(temp_individual_1);
            judge_constraint2 = judgeConstraintOne(temp_individual_2);

            if (!judge_constraint || !judge_constraint2) {
                temp = temp_individual_1[change_ap_list.get(i)][1];
                temp_individual_1[change_ap_list.get(i)][1] = temp_individual_2[change_ap_list.get(i)][1];
                temp_individual_2[change_ap_list.get(i)][1] = temp;
            }
        }

        //子となる個体を保存
        childrens[j].Parent1 = temp_individual_1;
        childrens[j].Parent2 = temp_individual_2;
        individuals.add(childrens[j].Parent1);
        individuals_evaluations.add(Evaluate(childrens[j].Parent1));
        individuals.add(childrens[j].Parent2);
        individuals_evaluations.add(Evaluate(childrens[j].Parent2));

    }


    /* 突然変異 : 個体を1つ選び、可能な限りチャネルoff*/
    private void Mutation() {
        int select_individual = rn.nextInt(individuals.size());
        int[][] mutate_individual = individuals.get(select_individual);
        int area_id;
        int[] channel_overray_check;    //1つのWiFi APに対して、カバーしているエリアごとの重複チャネル数をチェック

        //一旦、個体のチャネル割り当てをAPに適用
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = mutate_individual[i + Constants.LTEU_NUM][1];
        }

        ArrayList<Integer> cover_areas;
        ArrayList<Integer>[] area_covered_aps = _area.getAreaCooveredAPSet();

        //WiFi AP IDを0から見ていって、可能な限りoffにする
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            boolean not_change = false;
            cover_areas = _wifi_ap[i].cover_area_list;
            //カバーエリアを見て、APのチャネルをoffにできるかを判断
            channel_overray_check = new int[cover_areas.size()];
            for (int j = 0; j < cover_areas.size(); j++) {//WiFiのカバーエリアの探索
                area_id = cover_areas.get(j);
                for (int k = 0; k < area_covered_aps[area_id].size(); k++) { //最小エリアごとのAPの探索
                    if (_wifi_ap[area_covered_aps[area_id].get(k)].assigned_channel >= 0) {
                        channel_overray_check[j]++;
                    }
                }
            }
            //channel_overray_checkから、すべてのエリアを確認
            for (int j = 0; j < cover_areas.size(); j++) {
                if (channel_overray_check[j] <= 1) {
                    not_change = true;
                    break;//そのAPではチャネルoffにできない。
                }
            }

            if (!not_change) {
                _wifi_ap[i].assigned_channel = -1;
            }
        }

        //突然変異の個体を保存
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            mutate_individual[i + Constants.LTEU_NUM][1] = _wifi_ap[i].assigned_channel;
        }

        //突然変異した個体をもとの個体にコピー
        individuals.set(select_individual, mutate_individual);

    }

    /* 突然変異の条件: チャネルoffになるAPを持つ個体が存在しない */
    private boolean judgeMutation() {
        int[][] check_individual;
        for (int i = 0; i < individuals.size(); i++) {
            check_individual = individuals.get(i);
            for (int j = 0; j < check_individual.length; j++) {
                if (check_individual[j][1] == -1) {
                    return false;//チャネルoffのAPが1つでも見つかった時点で突然変異は起きない
                }
            }
        }
        return true;
    }

    /* 突然変異:すべての個体で最小スループットを示すAPが同じ場合、そのAPとそのカバーエリアにいるAPの割り当てをNeighborAssignで割り当てし直す。 */
    private void Mutation2() {
        int select_individual = rn.nextInt(individuals.size());
        int[][] mutate_individual = individuals.get(select_individual);
        int id = -1;

        int target_id = evaluations_ids.get(0);

        if (target_id < 0) {
            return;
        }

        //まず、対象のAP,BSとそのカバーエリアにいるやつらのチャネルをoffにする。
        if (target_id >= Constants.LTEU_NUM) {
            _wifi_ap[target_id - Constants.LTEU_NUM].assigned_channel = -1;

            for (int i = 0; i < _wifi_ap[target_id - Constants.LTEU_NUM].overray_list.size(); i++) {
                id = _wifi_ap[target_id - Constants.LTEU_NUM].overray_list.get(i);
                if (id < 10000) {
                    _wifi_ap[id].assigned_channel = -1;
                }

            }
        } else {

            for (int i = 0; i < _lteu_bs[target_id].overray_list.size(); i++) {
                id = _lteu_bs[target_id].overray_list.get(i);
                if (id < 10000) {
                    _wifi_ap[id].assigned_channel = -1;
                }

            }
        }

        //チャネル割り当ての実行
        if (target_id < Constants.LTEU_NUM) {
            for (int i = 0; i < _lteu_bs[target_id].overray_list.size(); i++) {
                id = _lteu_bs[target_id].overray_list.get(i);
                if (id < 10000) {
                    _wifi_ap[id].assigned_channel = ci.WiFiLeastInterference(id, _wifi_ap, _lteu_bs);
                }
            }

        } else {
            _wifi_ap[target_id - Constants.LTEU_NUM].assigned_channel = ci.WiFiLeastInterference(target_id - Constants.LTEU_NUM, _wifi_ap, _lteu_bs);

            for (int i = 0; i < _wifi_ap[target_id - Constants.LTEU_NUM].overray_list.size(); i++) {
                id = _wifi_ap[target_id - Constants.LTEU_NUM].overray_list.get(i);
                if (id < 10000) {
                    _wifi_ap[id].assigned_channel = ci.WiFiLeastInterference(id, _wifi_ap, _lteu_bs);
                }

            }
        }

        //干渉と容量のセット
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i);
            _wifi_ap[i].SetCapacity();
        }

        for (int j = 0; j < Constants.LTEU_NUM; j++) {
            _lteu_bs[j].interference_list = ci.LTEUCheck(j);
            _lteu_bs[j].SetCapacity();
        }

        //突然変異の個体を保存
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            mutate_individual[i + Constants.LTEU_NUM][1] = _wifi_ap[i].assigned_channel;
        }

        //突然変異した個体をもとの個体にコピー
        individuals.set(select_individual, mutate_individual);

    }

    /* 突然変異の条件: すべての個体で最小スループットを示すAPが一緒である場合 */
    private boolean judgeMutation2() {
        for (int i = 0; i < evaluations_ids.size() - 1; i++) {
            if (!Objects.equals(evaluations_ids.get(i), evaluations_ids.get(i + 1))) {
                return false;
            }
        }

        return true;
    }

    /* 淘汰 */
    private void Select() {
        LinkedList<int[][]> next_individuals = new LinkedList<>();

        // 2*Constants.PARENT_NUMに増えた個体数をConstants.INDIVIDUAL_NUMまで減らす
        //1.エリート選択で、Constants.ELITE_SELECT_NUM個の個体を残す
        for (int i = 0; i < Constants.ELITE_SELECT_NUM; i++) {
            next_individuals.add(EliteSelection());
        }

        //2.ルーレット選択によりConstants.INDIVIDUAL_NUM - Constants.ELITE_SELECT_NUM個の個体を残す
        double[] survival_probability = new double[individuals.size()]; //各個体の生存確率
        double sum = 0;
        double[] border = new double[individuals.size()];   //各個体間の生存確率の境界
        double temp_sum_border = 0;
        double roulette_select_value;

        for (int i = 0; i < individuals_evaluations.size(); i++) {
            sum += individuals_evaluations.get(i);
        }

        for (int i = 0; i < individuals_evaluations.size(); i++) {
            survival_probability[i] = individuals_evaluations.get(i) / sum;
            temp_sum_border += survival_probability[i];
            border[i] = temp_sum_border;
        }

        int select_index = 0;

        for (int j = 0; j < Constants.GA_INDIVIDUAL_NUM - Constants.ELITE_SELECT_NUM; j++) {
            do {
                roulette_select_value = rn.nextDouble();
            } while (roulette_select_value == 0);

            if (0 < roulette_select_value && roulette_select_value <= border[0]) {
                select_index = 0;
            } else {
                for (int i = 1; i < individuals.size(); i++) {
                    if (border[i - 1] < roulette_select_value && roulette_select_value <= border[i]) {
                        select_index = i;
                    }
                }
            }

            next_individuals.add(individuals.get(select_index));
        }

        //元の集団の個体と評価をクリア
        individuals.clear();
        individuals_evaluations.clear();

//        individuals = next_individuals;
        while (!next_individuals.isEmpty()) {
            individuals.add(next_individuals.poll());
        }
    }

    /* エリート選択*/
    private int[][] EliteSelection() {
        int[][] temp_individual;
        double temp_max_evaluation_value = 0;
        int temp_index = 0;

        for (int i = 0; i < individuals_evaluations.size(); i++) {
            if (temp_max_evaluation_value <= individuals_evaluations.get(i)) {
                temp_max_evaluation_value = individuals_evaluations.get(i);
                temp_index = i;
            }
        }//System.out.println(temp_max_evaluation_value);

        temp_individual = individuals.get(temp_index);
        individuals.remove(temp_index);
        individuals_evaluations.remove(temp_index); 

        return temp_individual;
    }

    /*個体の評価をする:最小スループットの最大化*/
    private double Evaluate(int[][] individual) {

        double[] temp_evaluation = new double[Constants.LTEU_NUM + Constants.WiFi_NUM];
        double temp_min_evaluataion = 999999;

        //1.個体のチャネル割り当て情報を記録
        for (int i = 0; i < Constants.LTEU_NUM; i++) {

            if (_lteu_bs[i].ap_id == individual[i][0]) {
                _lteu_bs[i].assigned_channel = individual[i][1];
            } else {
                System.out.println("LTEU_ERROR");
            }
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {

            if (_wifi_ap[i].ap_id == individual[i + Constants.LTEU_NUM][0]) {
                _wifi_ap[i].assigned_channel = individual[i + Constants.LTEU_NUM][1];
            } else {
                System.out.println("WiFi_ERROR");
            }
        }

        //2. 各AP, BSの容量をセット
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].interference_list = ci.LTEUCheck(i, _wifi_ap, _lteu_bs);
            _lteu_bs[i].SetCapacity();
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i, _wifi_ap, _lteu_bs);
            _wifi_ap[i].SetCapacity();
        }

        /* ユーザの接続先選択 */
        ProposedReConnect(individual);
//        ReConnectNotChange(individual);
        for (int i = 0; i < Constants.LTEU_NUM; i++) {

            if (_lteu_bs[i].connecting_num == 0) {
                temp_evaluation[i] = 1000000000;
            } else {
                temp_evaluation[i] = Utility.CalcUserthroughput(_lteu_bs[i].capacity, _lteu_bs[i].connecting_num);
            }

        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {

            if (_wifi_ap[i].connecting_num == 0) {
                temp_evaluation[i + Constants.LTEU_NUM] = 1000000000;
            } else {
                temp_evaluation[i + Constants.LTEU_NUM] = Utility.CalcUserthroughput(_wifi_ap[i].capacity, _wifi_ap[i].connecting_num);
            }
        }

        int temp_id = -1;
        //3. 評価値を決定:最小のスループット 
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            if (temp_min_evaluataion > temp_evaluation[i] && temp_evaluation[i] > 0) {
                temp_min_evaluataion = temp_evaluation[i];
                temp_id = i;

            }

        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            if (temp_min_evaluataion > temp_evaluation[i + Constants.LTEU_NUM] && temp_evaluation[i + Constants.LTEU_NUM] > 0) {
                temp_min_evaluataion = temp_evaluation[i + Constants.LTEU_NUM];
                temp_id = i + Constants.LTEU_NUM;
            }

        }

        evaluations_ids.add(temp_id);

        return temp_min_evaluataion;
    }

    /*個体の評価をする:平均スループットの最大化*/
    private double Evaluate2(int[][] individual) {

        double[] temp_evaluation = new double[Constants.LTEU_NUM + Constants.WiFi_NUM];
        double average_throughput = 0;
        double total_connecting_num = 0;

        //1.個体のチャネル割り当て情報を記録
        for (int i = 0; i < Constants.LTEU_NUM; i++) {

            if (_lteu_bs[i].ap_id == individual[i][0]) {
                _lteu_bs[i].assigned_channel = individual[i][1];
            } else {
                System.out.println("LTEU_ERROR");
            }
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {

            if (_wifi_ap[i].ap_id == individual[i + Constants.LTEU_NUM][0]) {
                _wifi_ap[i].assigned_channel = individual[i + Constants.LTEU_NUM][1];
            } else {
                System.out.println("WiFi_ERROR");
            }
        }

        //2. 各AP, BSの容量をセット
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].interference_list = ci.LTEUCheck(i, _wifi_ap, _lteu_bs);
            _lteu_bs[i].SetCapacity();
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].interference_list = ci.WiFiCheck(i, _wifi_ap, _lteu_bs);
            _wifi_ap[i].SetCapacity();
        }

        /* ユーザの接続先選択 */
        ProposedReConnect(individual);
//          ReConnectNotChange(individual);
        for (int i = 0; i < Constants.LTEU_NUM; i++) {

            if (_lteu_bs[i].connecting_num != 0) {

                temp_evaluation[i] = Utility.CalcUserthroughput(_lteu_bs[i].capacity, _lteu_bs[i].connecting_num);
                average_throughput += temp_evaluation[i] * _lteu_bs[i].connecting_num;
                total_connecting_num += _lteu_bs[i].connecting_num;

            }

        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {

            if (_wifi_ap[i].connecting_num != 0) {

                temp_evaluation[i + Constants.LTEU_NUM] = Utility.CalcUserthroughput(_wifi_ap[i].capacity, _wifi_ap[i].connecting_num);
                average_throughput += temp_evaluation[i + Constants.LTEU_NUM] * _wifi_ap[i].connecting_num;
                total_connecting_num += _wifi_ap[i].connecting_num;
            }
        }

        //3. 評価値を決定:平均のスループット 
        average_throughput = average_throughput / total_connecting_num;

        return average_throughput;
    }

    /* 提案手法のユーザの接続先変更 */
    private void ProposedReConnect(int[][] individual) {

        //エリアごとのユーザ数を保持
        int[] area_wifi_user_num = new int[Constants.AREA_NUM];
        int[] area_lteu_user_num = new int[Constants.AREA_NUM];

        //エリアごとのUserNodeを保持
        LinkedList<UserNode>[] area_wifi_users = new LinkedList[Constants.AREA_NUM];
        LinkedList<UserNode>[] area_lteu_users = new LinkedList[Constants.AREA_NUM];

        for (int i = 0; i < Constants.AREA_NUM; i++) {
            area_wifi_users[i] = new LinkedList<>();
            area_lteu_users[i] = new LinkedList<>();
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            if (!_wifi_ap[i].UserList.isEmpty()) {
                for (int j = 0; j < _wifi_ap[i].UserList.size(); j++) {
                    if (_wifi_ap[i].UserList.get(j).user_set == 0) {
                        area_wifi_users[_wifi_ap[i].UserList.get(j).getArea()].add(_wifi_ap[i].UserList.get(j));
                        area_wifi_user_num[_wifi_ap[i].UserList.get(j).getArea()] += 1;
                    } else {
                        area_lteu_users[_wifi_ap[i].UserList.get(j).getArea()].add(_wifi_ap[i].UserList.get(j));
                        area_lteu_user_num[_wifi_ap[i].UserList.get(j).getArea()] += 1;
                    }
                }
            }
        }

        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            if (!_lteu_bs[i].UserList.isEmpty()) {
                for (int j = 0; j < _lteu_bs[i].UserList.size(); j++) {
                    if (_lteu_bs[i].UserList.get(j).user_set == 1) {
                        area_lteu_users[_lteu_bs[i].UserList.get(j).getArea()].add(_lteu_bs[i].UserList.get(j));
                        area_lteu_user_num[_lteu_bs[i].UserList.get(j).getArea()] += 1;
                    } else {
                        System.out.println("LTE-u <- WiFi user ERROR");
                    }
                }
            }
        }

        //AP,BSのユーザ情報のクリア ＆個体のチャネル割り当てに更新
        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = individual[i + Constants.LTEU_NUM][1];
            _wifi_ap[i].UserList.clear();
            _wifi_ap[i].connecting_num = 0;
            _wifi_ap[i].user_throughput = 0;
        }

        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].assigned_channel = individual[i][1];
            _lteu_bs[i].UserList.clear();
            _lteu_bs[i].connecting_num = 0;
            _lteu_bs[i].user_throughput = 0;
        }

        //各エリアでどのAP,BSがそのエリアをカバーしているかのリスト
        ArrayList<Integer>[] ap_cover_set;
        ArrayList<Integer>[] bs_cover_set;

        ap_cover_set = _area.getAreaCooveredAPSet();
        bs_cover_set = _area.getAreaCooveredBSSet();

        //1. WiFi + LTEユーザを接続可能ならLTE-U BSに接続(BS同士はセルが重複していないと仮定)
//        for (int i = 0; i < Constants.AREA_NUM; i++) {//*********************************
//            if (bs_cover_set[i].size() != 0) {
//                for (int k = 0; k < bs_cover_set[i].size(); k++) {      //System.out.println(area_lteu_user_num[i]+"**--*--"+ area_lteu_users[i].size());
//                    while (!area_lteu_users[i].isEmpty()) {
//                        _lteu_bs[bs_cover_set[i].get(k)].UserList.add(area_lteu_users[i].poll());
//                    }                             // System.out.println(_lteu_bs[bs_cover_set[i].get(k)].UserList.size()+"\t" +area_lteu_users[i].size());
//                    Utility.ChangeConnectingAPs(_lteu_bs[bs_cover_set[i].get(k)]);
//                    _lteu_bs[bs_cover_set[i].get(k)].connecting_num += area_lteu_user_num[i];
//                    _lteu_bs[bs_cover_set[i].get(k)].reconnect(event_time, _param.service_set);
//                }
//            }
//        }//*****************************
        //BSが複数ある場合の処理****************************************************************
        APBSselection apbs_selection_try = new APBSselection(_scenario);
        int bs = -1;

        int bs_num = 1; //bs_numを--したら、 Constants.LTEU_AREA_COVER_NUMも--にならないよね
        while (bs_num != Constants.LTEU_AREA_COVER_NUM + 1) {
            for (int i = 0; i < Constants.AREA_NUM; i++) {
                if (bs_cover_set[i].size() == bs_num) {
                    while (!area_lteu_users[i].isEmpty()) {
                        bs = apbs_selection_try.SelectBS(area_lteu_users[i].peek());
                        area_lteu_users[i].peek().ChangeConnectedAP(_lteu_bs[bs - 10000]);
                        _lteu_bs[bs - 10000].UserList.add(area_lteu_users[i].poll());
                        _lteu_bs[bs - 10000].connecting_num++;
                        _lteu_bs[bs - 10000].reconnect(event_time, _param.service_set);
                    }

                }
            }
            bs_num++;
        }
        //終了*********************************************************

        PriorityQueue<APCover> aps_descender_area = new PriorityQueue<>(new MyComparator());
        //2.WiFiのみユーザ, LTE + WiFiユーザの接続先WiFi APを決定
        //接続先候補の少ないユーザ順に並べる⇒カバーAP数の少ないエリア順に並べることと等価
        APCover ap_cover;
        for (int i = 0; i < Constants.AREA_NUM; i++) {
            ap_cover = new APCover(i, ap_cover_set[i].size());
            aps_descender_area.add(ap_cover);
        }

        APBSselection apbs_selection = new APBSselection(_scenario);

        int reconnect_ap;
        while (!aps_descender_area.isEmpty()) {
            ap_cover = aps_descender_area.poll();
            for (int i = 0; i < area_wifi_users[ap_cover.getAreaId()].size(); i++) {
                reconnect_ap = apbs_selection.SelectAP(area_wifi_users[ap_cover.getAreaId()].get(i));
                area_wifi_users[ap_cover.getAreaId()].get(i).ChangeConnectedAP(_wifi_ap[reconnect_ap]);
                _wifi_ap[reconnect_ap].UserList.add(area_wifi_users[ap_cover.getAreaId()].get(i));
                _wifi_ap[reconnect_ap].connecting_num++;
                _wifi_ap[reconnect_ap].reconnect(event_time, _param.service_set);
            }

            //※すでに接続済みのやつはsize=0
            for (int i = 0; i < area_lteu_users[ap_cover.getAreaId()].size(); i++) {//※※ここでのLTE-UユーザはLTE-Uに接続できないやつだけ
                reconnect_ap = apbs_selection.SelectAPorBS(area_lteu_users[ap_cover.getAreaId()].get(i));
                if (reconnect_ap < 10000) {
                    area_lteu_users[ap_cover.getAreaId()].get(i).ChangeConnectedAP(_wifi_ap[reconnect_ap]);
                    _wifi_ap[reconnect_ap].UserList.add(area_lteu_users[ap_cover.getAreaId()].get(i));
                    _wifi_ap[reconnect_ap].connecting_num++;
                    _wifi_ap[reconnect_ap].reconnect(event_time, _param.service_set);
                } else {
                    System.out.println("RECONNECT_ERROR");
                    area_lteu_users[ap_cover.getAreaId()].get(i).ChangeConnectedAP(_lteu_bs[reconnect_ap]);
                    _lteu_bs[reconnect_ap - 10000].UserList.add(area_lteu_users[ap_cover.getAreaId()].get(i));
                    _lteu_bs[reconnect_ap - 10000].connecting_num++;
                    _lteu_bs[reconnect_ap - 10000].reconnect(event_time, _param.service_set);
                }
            }
        }

        //3.LTE-U→WiFiへのオフロード(LTE-UのID順に見て、オフロードユーザを選択)(※候補が少ないところから選んだほうが良いかも)
        int offload_ap;
        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            for (int j = 0; j < _lteu_bs[i].UserList.size(); j++) {
                offload_ap = apbs_selection.SelectAPorBSforAlgorithm(_lteu_bs[i].UserList.get(j));
                if (offload_ap < 10000) {//LTE-U BS同士は重複していないことを前提にしている
                    _lteu_bs[i].UserList.get(j).ChangeConnectedAP(_wifi_ap[offload_ap]);
                    _wifi_ap[offload_ap].UserList.add(_lteu_bs[i].UserList.get(j));
                    _wifi_ap[offload_ap].connecting_num++;
                    _wifi_ap[offload_ap].reconnect(event_time, _param.service_set);

                    _lteu_bs[i].UserList.remove(j);
                    _lteu_bs[i].connecting_num--;
                    _lteu_bs[i].reconnect(event_time, _param.service_set);
                    j--;
                }
            }
        }
    }

    /* 接続先を変更しない場合 */
    private void ReConnectNotChange(int[][] individual) {

        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].reconnect(event_time, _param.service_set);
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].assigned_channel = individual[i + Constants.LTEU_NUM][1];
            _wifi_ap[i].reconnect(event_time, _param.service_set);
        }

    }

    /* 初期個体の評価で誤った最小スループットが入らないようにする */
    private void FirstReConnect() throws IOException {

        for (int i = 0; i < Constants.LTEU_NUM; i++) {
            _lteu_bs[i].reconnect(event_time, _param.service_set);

            lte_num += _lteu_bs[i].connecting_num;
//System.out.println(_lteu_bs[i].user_throughput);
        }

        for (int i = 0; i < Constants.WiFi_NUM; i++) {
            _wifi_ap[i].reconnect(event_time, _param.service_set);

            for (int x = 0; x < _wifi_ap[i].UserList.size(); x++) {
                if (_wifi_ap[i].UserList.get(x).user_set == 1) {
                    wifi_num++;
                }
//System.out.println(_wifi_ap[i].user_throughput + "****");
            }
        }

//        if (_param.loop_num == 1) {
//            writeToFile_wifi_lte_num();
//        }
//        System.out.println(wifi_num + "\t" + lte_num);
    }

    //カバーしているAPが少ないエリア順に
    static class MyComparator implements Comparator<APCover> {

        @Override
        public int compare(APCover arg0, APCover arg1) {
            APCover x = arg0;
            APCover y = arg1;

            if (x.getCoverAPNum() > y.getCoverAPNum()) {
                return 1;
            } else if (x.getCoverAPNum() < y.getCoverAPNum()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    //干渉が多い順に並べる
    static class MyComparator2 implements Comparator<InterferenceOrder> {

        @Override
        public int compare(InterferenceOrder arg0, InterferenceOrder arg1) {
            InterferenceOrder x = arg0;
            InterferenceOrder y = arg1;

            if (x.inteference_num < y.inteference_num) {
                return 1;
            } else if (x.inteference_num > y.inteference_num) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public void writeToFile_wifi_lte_num() throws IOException {
        FileWriter fw = new FileWriter("./WiFiLTEU_Data/NumNum/" + Constants.are + "num.dat", true);
        BufferedWriter bw = new BufferedWriter(fw);

        String text = wifi_num + "\t" + lte_num + "\n";

        bw.write(text);

        bw.close();
    }

}
