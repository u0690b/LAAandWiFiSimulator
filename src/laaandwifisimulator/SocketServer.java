package laaandwifisimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class SocketServer {

    public static UserParameter _param;

    public static void main(String[] args) throws IOException {

        AreaTopology topology = initSimulator();
        int port = 8888;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            Socket socket = serverSocket.accept();
            while (true) {
                System.out.println("New client connected");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String payload = reader.readLine();
                String[] tokens = payload.split(",");
                int seq = Integer.parseInt(tokens[0]);
                int[] channels = new int[107];
                for (int i = 1; i < tokens.length; i++) {
                    channels[i - 1] = Integer.parseInt(tokens[i]);
                }

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String res = seq + "," + runSimulator(topology, channels);

                writer.println(res);
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static AreaTopology initSimulator() throws IOException {

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
        LAAandWiFiSimulator._param = _param;
        // シミュレーション結果をファイル出力するクラスの作成
        Constants.are = args1[2];
        System.out.println("Capacities installing.... ");

        // チャネル共有時の容量をセット
        Constants.CAPACITY_WITH_LAA_WIFI = Utility.SetCapacitySharedWiFiLTEU();
        Constants.CAPACITY_WITH_WIFIS = Utility.SetCapacitySharedWiFi();

        System.out.println("Topology installing....");

        // エリア、AP,BSのカバー範囲, LTE-Uの配置場所を取得
        return new AreaTopology();

    }

    public static double runSimulator(AreaTopology topology, int[] channels) throws IOException {
        long start = System.currentTimeMillis();// 時間計測
        // シナリオの作成
        Output output = new Output(_param);
        Scenario scenario;

        Constants.SERVICE_SET = _param.service_set;

        System.out.println("Loop " + (1 + 1));
        scenario = new Scenario(1 + 30, _param, topology, channels);

        scenario.startSimulation();
        output.update(scenario);

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

        return scenario.getData().ave_throughput;
    }
}
