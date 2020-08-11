package edu.nju.autodroid.main;

import com.android.ddmlib.*;
import edu.nju.autodroid.androidagent.AdbAgent;
import edu.nju.autodroid.androidagent.IAndroidAgent;
import edu.nju.autodroid.androidagent.UiAutomationAgent;
import edu.nju.autodroid.strategy.*;
import edu.nju.autodroid.uiautomator.UiautomatorClient;
import edu.nju.autodroid.utils.AdbTool;
import edu.nju.autodroid.utils.Configuration;
import edu.nju.autodroid.utils.Logger;
import edu.nju.autodroid.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Created by ysht on 2016/9/26.
 */
public class Main_Single {
    public static void main(String[] args) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, InterruptedException {
        if(args.length != 2 ){
            System.out.println("Usage: java -jar AutoDroid.jar <Mode> <APK-Folder-path>");
            return;
        }
        int mode = Integer.parseInt(args[0]);
        if(mode == 0)
        {
            Main.main(args);
            return;
        }

        DdmPreferences.setTimeOut(10000);
        AdbTool.initializeBridge();

        List<String> apkFileList = Main.getApkFileList(args[1]);
        Logger.logInfo("Total Apk counts：" + apkFileList.size());

        IDevice device = AdbTool.getDefaultDevice();//使用默认的device
//        List<IDevice> devicesList = AdbTool.getDevices();//获得所有的devices
//        int size = devicesList.size();
//        int[] synclock = new int[size];
//        for(int i = 0;i<size;i++)//初始化多个设备的锁
//            synclock[i] = 0;


        for(String apkFilePath : apkFileList){
            List<String> finishedList = Main.getFinishedList("finishedList_0.txt");
            if(finishedList.contains(apkFilePath))
                continue;

//            for(int i = 0;i<size;i++){
//                if(synclock[i] == 0){
//                    IDevice device = devicesList.get(i);
//                    synclock[i] = 1;
//                    break;
//                }
//            }


            String[] fileinfo = apkFilePath.split("\\\\");
            String graph_folder = fileinfo[fileinfo.length-2];
            File apkFile = new File(apkFilePath);
            IAndroidAgent agent = new AdbAgent(device, UiautomatorClient.PHONE_PORT, UiautomatorClient.PHONE_PORT);//new UiAutomationAgent(device, 22233, 22233);//new AdbAgent(device, UiautomatorClient.PHONE_PORT, UiautomatorClient.PHONE_PORT);//
            boolean result;
            result = agent.init();
            Logger.logInfo("Init agent："+result);
            String packageName = AdbTool.getPackageFromApk(apkFilePath);
           // if(!AdbTool.hasInstalledPackage(agent.getDevice(), packageName))
            {
                result = AdbTool.installApk(agent.getDevice().getSerialNumber(), apkFilePath);
                Logger.logInfo("Install apk："+result);
            }

            if(result){
                String laubchableActivity = AdbTool.getLaunchableAcvivity(apkFilePath);

                if(!laubchableActivity.endsWith("/")) {
                    String apkName = apkFile.getName().substring(0, apkFile.getName().lastIndexOf('.'));
                    //创建文件夹保存截图
                    File screenshotFolder = new File("screenshot\\"+apkName);
                    if (!screenshotFolder.exists() && !screenshotFolder.isDirectory()) {
                        System.out.println("文件夹路径不存在，创建路径:" + apkName);
                        screenshotFolder.mkdirs();
                    } else {
                        System.out.println("文件夹路径存在:" + apkName);
                    }

                    IStrategy strategy = new GridWeightedStrategy(agent, Configuration.getMaxStep(), laubchableActivity, new Logger(apkName, "logger_output_"+graph_folder+"\\" + apkName + ".txt"));//"com.financial.calculator/.FinancialCalculators"
                    Logger.logInfo("Start Strategy：" + strategy.getStrategyName());
                    Logger.logInfo("Strategy target：" + apkFilePath);
                    try{
                        if (strategy.run()) {
                            Logger.logInfo("Strategy finished successfully！");
                        } else {
                            Logger.logInfo("Strategy finished with errors！");
                        }

                    }
                    catch (Exception e){
                        Logger.logException("Strategy can't finish！");
                        e.printStackTrace();
                    }
                    strategy.writeToFile("strategy_output_"+graph_folder+"\\" + apkName);
                }else{
                    Logger.logInfo("Can not get Launchable Activity");
                }

                AdbTool.unInstallApk(agent.getDevice().getSerialNumber(), packageName);
            }

            agent.terminate();
            finishedList.add(apkFilePath);
            Main.setFinishedList("finishedList_0.txt", finishedList);


            Thread.sleep(2000);
        }

        AdbTool.terminateBridge();
        Logger.endLogging();

    }
}
