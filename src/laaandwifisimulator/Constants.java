/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package laaandwifisimulator;

/**
 * 定数を扱うstaticでfinalなクラス
 * @author ginnan
 */
public class Constants {
    
    //エリア情報に関する情報
    static int AREA_NUM = 288;  //最小エリアの数 ※288 ※72
    static int WiFi_NUM = 100;  //WiFi APの数 ※100 ※28
    static int LTEU_NUM = 7;   //LTE-U BSの数 ※10 ※2 ※8 ※3 ※7
    static int LINE_NUM = 2;   //エリアの列の数(最小エリア数で割り切れる数を選択) /* 使われていない*/
    
    static int AP_COVER_NUM = 54;     //WiFi APがカバーしているエリアの数 ※24  ※54
    static int BS_COVER_NUM = 54;     //LTE-U BSがカバーしているエリアの数 ※24 ※54
    static int AP_BS_POSITION_NUM = 136;    //WiFi ,LTE-Uを置ける位置の数 ※136 ※31 ※136
    static int AREA_COVERED_AP_NUM = 12; //最小エリアをカバーしているAPの数(APがすべての位置に配置された場合を仮定) ※12 ※12
    static int AREA_COVERED_BS_NUM = 12; //最小エリアをカバーしているBSの数(BSがすべての位置に配置された場合を仮定) ※12 ※12
    
    static int LTEU_AREA_COVER_NUM = 2; //同一エリアをカバーしているLTE-Uの最大数
    
    //WiFi AP or LTE-U BSに関する情報
    static double WiFi_CAPACITY = 40; //WiFi AP の容量[Mbps]
    static double LTEU_CAPACITY = 75; //LTE-U BS の容量[Mbps]
    static int CHANNEL_NUM = 4; //利用可能なチャネルの数 ※8
    static double[][] CAPACITY_WITH_LAA_WIFI;   //WiFiとLTE-Uがチャネルを共有するときの容量[Mbps]
    static double[] CAPACITY_WITH_WIFIS;        //WiFi同士がチャネルを共有するときの容量[Mbps]
    static int WiFi_CSMA_RANGE = 54; //CSMA/CAが可能な範囲(通信可能な範囲) ※54
    static int LTEU_LBT_RANGE = 54;  //LBTが可能な範囲(通信可能な範囲) ※54
            
    //ユーザの情報
    static double DOWNLOAD_FILE_SIZE = 400; //ダウンロードファイルサイズ [Mbit]
    static double mu = 0.00333333333333333; //0.001666667;  //0.0166666666666666666; //0.00333333333333333; //0.008333333333;////平均通信時間の逆数
    static double STEADY_NUM = 10000;    //過渡状態の呼数
    
    //遺伝的アルゴリズムのパラメータ→引数で入力できるようにしているので、使っていないです
    static final int GA_NUM = 1000;					// GAのループ回数
    static final int GA_INDIVIDUAL_NUM = 10;//※10				// GAの初期集団数    
    static final int PARENT_NUM = 3;//※3 //一回の交叉で選ぶ親の組の数
    static final int CROSSOVER_NUM = 1;
    static final int ELITE_SELECT_NUM = 1; //淘汰において、エリート選択で淘汰する個体数

    static int SERVICE_SET;//一定時間通信か、ファイルダウンロードか
    
    static String VERSION = Constants.class.getPackage().getName();
    
    static String are; //ファイル出力のファイル名に使っている
    
//    static int num = 1;
}
