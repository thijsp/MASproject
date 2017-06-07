package simulation;

import au.com.bytecode.opencsv.CSVWriter;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;

/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentResult {

    public List<ArrayList<Integer>> results;
    public List<List<Long>> parcelTimes;
    private List<Long> deliveredAverages;
    private List<Long> existanceAverages;


    ExperimentResult(int steps, int measurements) {
        this.results = new ArrayList<>();
        this.deliveredAverages = new ArrayList<>();
        this.existanceAverages = new ArrayList<>();
        this.parcelTimes = new ArrayList<>();
    }

    public void report(ArrayList<Integer> runResult) {
        this.results.add(runResult);
    }

    public void saveResult() {
        String dir = System.getProperty("user.dir");
        String csv = dir + "/output.csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv));
            for (ArrayList<Integer> result : results) {
                writer.writeNext(getStringArray(result));
            }
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAverages() {
        String dir = System.getProperty("user.dir");
        String csv = dir + "/avg.csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv));
            writer.writeNext(getStringLongArray(deliveredAverages));
            writer.flush();
            writer.writeNext(getStringLongArray(existanceAverages));
            writer.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getStringArray(ArrayList<Integer> result) {
        String[] values = new String[result.size()];
        for (int i =0; i < values.length; i++) {
            values[i] = result.get(i).toString();
        }
        return values;
    }

    public String[] getStringLongArray(List<Long> result) {
        String[] values = new String[result.size()];
        for (int i =0; i < values.length; i++) {
            values[i] = result.get(i).toString();
        }
        return values;
    }

    public void reportParcelTime(List<Long> times) {
        this.parcelTimes.add(times);
    }

    public void saveParcelTimes() {
        String dir = System.getProperty("user.dir");
        String csv = dir + "/parceltimes.csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv));
            List<String[]> everything = new ArrayList<>();
            for (List<Long> result : this.parcelTimes) {
                String[] writing = getStringLongArray(result);
                everything.add(writing);
            }
            writer.writeAll(everything);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reportDeliveredAverage(long avg) {
        this.deliveredAverages.add(avg);
    }

    public void reportExistanceAverage(long avg) {
        this.existanceAverages.add(avg);
    }


    public List<Long> getDeliveredAverages() {
        return this.deliveredAverages;
    }

    public List<Long> getExistanceAverages() { return this.existanceAverages; }
}
