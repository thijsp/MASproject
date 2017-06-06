package simulation;

import au.com.bytecode.opencsv.CSVWriter;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;

/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentResult {

    private List<ArrayList<Integer>> results;
    private List<Long> deliveredAverages;
    private List<Long> existanceAverages;


    ExperimentResult(int steps, int measurements) {
        this.results = new ArrayList<>();
        this.deliveredAverages = new ArrayList<>();
        this.existanceAverages = new ArrayList<>();
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
