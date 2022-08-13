package ru.ivetta;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FileMergeSorter {

    public static void main(String[] args) {
        Configuration config = Configuration.parseConfiguration(args);
        Path outFilePath = Path.of(config.outFile);
        try {
            List<String> sortedList = sortFiles(config);
            Files.write(outFilePath, sortedList, Charset.defaultCharset());
        } catch (Exception e) {
            try {
                if (e.getMessage() == null)
                    e.printStackTrace();
                else
                    Files.write(outFilePath, List.of(e.getMessage()), Charset.defaultCharset());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static List<String> sortFiles(Configuration config) throws IOException {
        List<BufferedReader> fileReaderList = getBufferedReaderList(config);
        List<String> sortedList = new ArrayList<>();
        while (!fileReaderList.isEmpty()) {
            // сортируем элементы из входящих коллекций, а потом вставляем в конечный массив
            String limitValue = config.limitValue();
            boolean isMinValueSet = false;
            BufferedReader limitValueFileReader = null;
            for (Iterator<BufferedReader> iterator = fileReaderList.iterator(); iterator.hasNext(); ) {
                BufferedReader fileReader = iterator.next();
                fileReader.mark(1000);
                String value = fileReader.readLine(); // читаем все по одному разу
                if (value == null) {
                    // если в этом файле закончились строчки, то закрываем его и удяляем из итерации
                    fileReader.close();
                    iterator.remove();
                    continue;
                }
                if (config.compare(value, limitValue)) {
                    limitValue = value;
                    isMinValueSet = true;
                    if (limitValueFileReader == null) {
                        limitValueFileReader = fileReader; // устанавливаем минимальный fileReader
                    } else {
                        limitValueFileReader.reset(); // откатываем прошлый минимальный
                        limitValueFileReader = fileReader; //ставим текущий
                    }
                } else {
                    fileReader.reset(); // если в этом файле символ не минимальный, то отматываем назад
                }
            }
            // добавляем наименьший элемент в результирующий массив
            if (isMinValueSet) sortedList.add(limitValue);
        }
        return sortedList;
    }

    private static List<BufferedReader> getBufferedReaderList(Configuration config) throws FileNotFoundException {
        List<BufferedReader> fileReaderList = new ArrayList<>();
        for (String inFile : config.inFiles) {
            BufferedReader reader =
                    new BufferedReader(new FileReader(inFile));
            fileReaderList.add(reader);
        }
        return fileReaderList;
    }

    private static class Configuration {
        private final SortType sortType;
        private final DataType dataType;
        private final String outFile;
        private final List<String> inFiles;

        private Configuration(
                SortType sortType,
                DataType dataType,
                String outFile,
                List<String> inFiles
        ) {
            this.sortType = sortType;
            this.dataType = dataType;
            this.outFile = outFile;
            this.inFiles = inFiles;
        }

        public static Configuration parseConfiguration(String[] args) {
            SortType sortType = SortType.ASCENDING;
            DataType dataType = null;

            // чтение аргументов
            Object[] arguments = Arrays.stream(args)
                    .filter(arg -> arg.startsWith("-")).toArray();

            for (Object arg : arguments) {
                String string = arg.toString();
                switch (string) {
                    case "-a" -> sortType = SortType.ASCENDING;
                    case "-d" -> sortType = SortType.DESCENDING;
                    case "-s" -> dataType = DataType.STRING;
                    case "-i" -> dataType = DataType.INTEGER;
                }
            }

            if (dataType == null) {
                throw new RuntimeException("Data type is not specified");
            }

            String outFile = null;
            List<String> inFiles = new ArrayList<>();
            for (String arg : args) {
                if (arg.endsWith(".txt")) {
                    if (outFile == null) {
                        outFile = arg;
                    } else {
                        inFiles.add(arg);
                    }
                }
            }
            if (outFile == null) {
                throw new RuntimeException("Missing output file");
            }
            if (inFiles.isEmpty()) {
                throw new RuntimeException("Missing input file");
            }

            return new Configuration(sortType, dataType, outFile, inFiles);
        }

        public String limitValue() {
            return switch (dataType) {
                case STRING -> sortType == SortType.ASCENDING
                        ? String.valueOf(Character.MAX_VALUE)
                        : String.valueOf(Character.MIN_VALUE);
                case INTEGER -> sortType == SortType.ASCENDING
                        ? String.valueOf(Integer.MAX_VALUE)
                        : String.valueOf(Integer.MIN_VALUE);
            };
        }

        public boolean compare(String value, String limitValue) {
            return switch (dataType) {
                case STRING -> sortType == SortType.ASCENDING
                        ? value.compareTo(limitValue) < 0
                        : value.compareTo(limitValue) > 0;
                case INTEGER -> sortType == SortType.ASCENDING
                        ? Integer.parseInt(value) < Integer.parseInt(limitValue)
                        : Integer.parseInt(value) > Integer.parseInt(limitValue);
            };
        }
    }

    private enum SortType {
        ASCENDING, DESCENDING
    }

    private enum DataType {
        STRING, INTEGER
    }
}
