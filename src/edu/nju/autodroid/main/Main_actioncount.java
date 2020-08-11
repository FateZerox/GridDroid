package edu.nju.autodroid.main;

import java.io.*;

public class Main_actioncount {
    public static void main(String[] args)throws IOException {
        File strategyFolder1 = new File("C:\\Users\\fatesqw\\Desktop\\GridDroid\\NoChangeCountLogger");
        for (File f : strategyFolder1.listFiles(new FileFilter(){
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory();
            }
        })){
            BufferedReader bw = new BufferedReader(new FileReader(f));
            String line;
            int count = 0;
            while ((line = bw.readLine()) != null) {
                count++;
            }
            System.out.println(f.getName()+" "+count);
        }
    }
}
