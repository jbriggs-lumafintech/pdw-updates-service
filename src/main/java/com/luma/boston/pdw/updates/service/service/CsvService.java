package com.luma.boston.pdw.updates.service.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
public class CsvService {

    public List<CSVRecord> readCsv(File file) {
        try (var fileReader = new FileReader(file)) {
            var csv = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(fileReader);
            return csv.getRecords();
        } catch (IOException e) {
            System.out.println("\n\n**************** Yo shit is broke ****************.\n\n");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<String> readHeaders(File file) {
        try (var fileReader = new FileReader(file)) {
            var csv = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(fileReader);
            return csv.getHeaderNames();
        } catch (IOException e) {
            System.out.println("\n\n**************** Yo shit is broke ****************.\n\n");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
