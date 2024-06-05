package org.molgenis.strstats;

import ch.qos.logback.classic.Level;
import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.cli.*;
import org.molgenis.strstats.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.molgenis.strstats.model.Strand.MIN;
import static org.molgenis.strstats.model.Strand.PLUS;

@Component
class AppCommandLineRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppCommandLineRunner.class);

    private static final int STATUS_MISC_ERROR = 1;
    private static final int STATUS_COMMAND_LINE_USAGE_ERROR = 64;

    private final String appName;
    private final String appVersion;
    private final CommandLineParser commandLineParser;

    AppCommandLineRunner(@Value("${app.name}") String appName, @Value("${app.version}") String appVersion) {
        this.appName = requireNonNull(appName);
        this.appVersion = requireNonNull(appVersion);

        this.commandLineParser = new DefaultParser();
    }

    @Override
    public void run(String... args) {
        if (args.length == 1 && (args[0].equals("-" + AppCommandLineOptions.OPT_VERSION) || args[0].equals("--" + AppCommandLineOptions.OPT_VERSION_LONG))) {
            LOGGER.info("{} {}", appName, appVersion);
            return;
        }

        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (!(rootLogger instanceof ch.qos.logback.classic.Logger)) {
            throw new ClassCastException("Expected root logger to be a logback logger");
        }
        ((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.INFO);

        CommandLine commandLine = getCommandLine(args);
        AppCommandLineOptions.validateCommandLine(commandLine);

        try {
            Path tsvPath = Path.of(commandLine.getOptionValue(AppCommandLineOptions.OPT_INPUT_TSV));
            Path bedPath = Path.of(commandLine.getOptionValue(AppCommandLineOptions.OPT_INPUT_BED));

            calcStats(tsvPath, bedPath, getOutput(commandLine));

        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            System.exit(STATUS_MISC_ERROR);
        }
    }

    private void calcStats(Path tsvPath, Path bedPath, Path output) {
        List<BedLine> bedLines = readBedFile(bedPath);
        List<TsvLine> tsvLines = readTsvFile(tsvPath);
        Map<Key, Integer> counts = new HashMap<>();
        for (TsvLine tsvLine : tsvLines) {
            Key key = Key.builder().chrom(tsvLine.getChrom()).pos(tsvLine.getPos()).allele(tsvLine.getAllele()).ru(tsvLine.getRepeatUnit()).strand(tsvLine.getStrand().equals("-") ? MIN : PLUS).build();
            if (counts.containsKey(key)) {
                Integer count = counts.get(key);
                count++;
                counts.put(key, count);
            } else {
                counts.put(key, 1);
            }
        }
        Map<String, Map<String, Allele>> allelesPerStr = new HashMap<>();
        List<OutputLine> outputLines = new ArrayList<>();
        for (BedLine bedLine : bedLines) {
            createAllelesPerString(counts, allelesPerStr, bedLine);
            OutputLine.OutputLineBuilder outputLine = OutputLine.builder();
            Map<String, Allele> alleles = allelesPerStr.get(bedLine.getChrom()+":"+bedLine.getPos());
            outputLine.chrom(bedLine.getChrom());
            outputLine.pos(bedLine.getPos());
            outputLine.id(bedLine.getId());
            if(alleles != null) {
                List<Allele> values = alleles.values().stream().toList();
                Allele allele1 = values.size() > 0 ? values.get(0) : null;
                Allele allele2 = values.size() > 1 ? values.get(1) : null;
                if(values.size() > 2){
                    throw new RuntimeException("More than 2 alleles encountered.");
                }
                if (allele1 != null) {
                    outputLine.allele1_ru(allele1.getRu());
                    if (allele1.getMinValue() != null) outputLine.allele1_min(allele1.getMinValue().toString());
                    if (allele1.getPlusValue() != null) outputLine.allele1_plus(allele1.getPlusValue().toString());
                }
                if (allele2 != null) {
                    outputLine.allele2_ru(allele2.getRu());
                    if (allele2.getMinValue() != null) outputLine.allele2_min(allele2.getMinValue().toString());
                    if (allele2.getPlusValue() != null) outputLine.allele2_plus(allele2.getPlusValue().toString());
                }
            }
            outputLines.add(outputLine.build());
        }
        writeToFile(output, outputLines);
    }

    private static void createAllelesPerString(Map<Key, Integer> counts, Map<String, Map<String, Allele>> allelesPerStr, BedLine bedLine) {
        Set<Map.Entry<Key, Integer>> countsForStr = counts.entrySet().stream().filter(value -> (value.getKey().getChrom().equals(bedLine.getChrom()) && value.getKey().getPos().equals(bedLine.getPos()))).collect(Collectors.toSet());
        Map<String, Allele> alleles;
        for(Map.Entry<Key, Integer> entry : countsForStr){
            Key key = entry.getKey();
            String stringKey = key.getChrom()+":"+key.getPos();
            alleles = allelesPerStr.get(stringKey);
            Allele allele = Allele.builder().ru(key.getRu()).build();
            if(allelesPerStr.containsKey(stringKey)){
                if(alleles.containsKey(key.getAllele())){
                    allele = alleles.get(key.getAllele());
                }
                if(key.getStrand() == MIN){
                    allele.setMinValue(entry.getValue());
                }else{
                    allele.setPlusValue(entry.getValue());
                }
            }else{
                alleles = new HashMap<>();
                allele = Allele.builder().ru(key.getRu()).build();
                if(key.getStrand() == MIN){
                    allele.setMinValue(entry.getValue());
                }else{
                    allele.setPlusValue(entry.getValue());
                }
            }
            alleles.put(key.getAllele(), allele);
            allelesPerStr.put(stringKey, alleles);
        }
    }

    private CommandLine getCommandLine(String[] args) {
        CommandLine commandLine = null;
        try {
            commandLine = commandLineParser.parse(AppCommandLineOptions.getAppOptions(), args);
        } catch (ParseException e) {
            logException(e);
            System.exit(STATUS_COMMAND_LINE_USAGE_ERROR);
        }
        return commandLine;
    }

    private Path getOutput(CommandLine commandLine) {
        String output;
        output = commandLine.getOptionValue(AppCommandLineOptions.OPT_INPUT_TSV).replace(".tsv", ".reads.tsv");
        return Path.of(output);
    }

    @SuppressWarnings("java:S106")
    private void logException(ParseException e) {
        LOGGER.error(e.getLocalizedMessage(), e);

        // following information is only logged to system out
        System.out.println();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        String cmdLineSyntax = "java -jar " + appName + ".jar";
        formatter.printHelp(cmdLineSyntax, AppCommandLineOptions.getAppOptions(), true);
        System.out.println();
        formatter.printHelp(cmdLineSyntax, AppCommandLineOptions.getAppVersionOptions(), true);
    }

    private static List<BedLine> readBedFile(Path input) {
        List<BedLine> bedLines;
        try (Reader reader = Files.newBufferedReader(input, UTF_8)) {

            CsvToBean<BedLine> csvToBean = new CsvToBeanBuilder<BedLine>(reader).withSeparator('\t')
                    .withType(BedLine.class).withThrowExceptions(false)
                    .build();

            ColumnPositionMappingStrategy<BedLine> mappingStrategy = new ColumnPositionMappingStrategy<>();
            mappingStrategy.setType(BedLine.class);
            mappingStrategy.setColumnMapping("chrom", "pos", "end", "repeat", "id", "id2");
            csvToBean.setMappingStrategy(mappingStrategy);

            bedLines = csvToBean.parse();
            handleCsvParseExceptions(csvToBean.getCapturedExceptions());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bedLines;
    }

    private static List<TsvLine> readTsvFile(Path input) {
        List<TsvLine> tsvLines;
        try (Reader reader = Files.newBufferedReader(input, UTF_8)) {

            CsvToBean<TsvLine> csvToBean = new CsvToBeanBuilder<TsvLine>(reader)
                    .withSkipLines(1)
                    .withSeparator('\t').withType(TsvLine.class)
                    .withThrowExceptions(false).build();
            tsvLines = csvToBean.parse();
            handleCsvParseExceptions(csvToBean.getCapturedExceptions());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return tsvLines;
    }

    static void handleCsvParseExceptions(List<CsvException> exceptions) {
        exceptions.forEach(csvException -> {
            // ignore errors parsing trailing comment lines
            if (!(csvException.getLine()[0].startsWith("#"))) {
                LOGGER.error(String.format("%s,%s", csvException.getLineNumber(), csvException.getMessage()));
            }
        });
    }

    private static void writeToFile(Path output, Collection<OutputLine> lines) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(output.toString()), '\t', DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END)) {
            writer.writeNext(new String[]{"Chrom", "Pos", "Id", "Allele1_RU", "Allele2_RU", "Allele1_min", "Allele1_plus", "Allele2_min", "Allele2_plus"}, false);
            lines.forEach(line -> writer.writeNext(new String[]{line.getChrom(), line.getPos(), line.getId(), line.getAllele1_ru(), line.getAllele2_ru(), line.getAllele1_min(), line.getAllele1_plus(), line.getAllele2_min(), line.getAllele2_plus()}, false));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
