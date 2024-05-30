package org.molgenis.strstats;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

class AppCommandLineOptions {

  static final String OPT_INPUT_TSV = "i";
  static final String OPT_INPUT_TSV_LONG = "tsv-input";
  static final String OPT_INPUT_BED = "b";
  static final String OPT_INPUT_BED_LONG = "bed-input";

  private static final Options APP_OPTIONS;
  private static final Options APP_VERSION_OPTIONS;
  static final String OPT_VERSION = "v";
  static final String OPT_VERSION_LONG = "version";

  static {
    Options appOptions = new Options();
    appOptions.addOption(
        Option.builder(OPT_INPUT_TSV)
            .hasArg(true)
            .longOpt(OPT_INPUT_TSV_LONG)
            .desc("Input straglr tsv.")
            .build());
    appOptions.addOption(
        Option.builder(OPT_INPUT_BED)
            .hasArg(true)
            .required()
            .longOpt(OPT_INPUT_BED_LONG)
            .desc("Input bed file.")
            .build());
    APP_OPTIONS = appOptions;
    Options appVersionOptions = new Options();
    appVersionOptions.addOption(
        Option.builder(OPT_VERSION)
            .required()
            .longOpt(OPT_VERSION_LONG)
            .desc("Print version.")
            .build());
    APP_VERSION_OPTIONS = appVersionOptions;
  }

  private AppCommandLineOptions() {}

  static Options getAppOptions() {
    return APP_OPTIONS;
  }

  static Options getAppVersionOptions() {
    return APP_VERSION_OPTIONS;
  }

  static void validateCommandLine(CommandLine commandLine) {
    validateInput(commandLine);
  }

  private static void validateInput(CommandLine commandLine) {
    if (commandLine.hasOption(OPT_INPUT_TSV)) {
      validateFile(commandLine, OPT_INPUT_TSV, ".tsv");
    }
    if (commandLine.hasOption(OPT_INPUT_BED)) {
      validateFile(commandLine, OPT_INPUT_BED, ".bed");
    }
  }

  private static void validateFile(CommandLine commandLine, String option, String extension) {
    Path inputPath = Path.of(commandLine.getOptionValue(option));
    if (!Files.exists(inputPath)) {
      throw new IllegalArgumentException(
          format("Input file '%s' does not exist.", inputPath));
    }
    if (Files.isDirectory(inputPath)) {
      throw new IllegalArgumentException(
          format("Input file '%s' is a directory.", inputPath));
    }
    if (!Files.isReadable(inputPath)) {
      throw new IllegalArgumentException(
          format("Input file '%s' is not readable.", inputPath));
    }
    String inputPathStr = inputPath.toString();
    if (!inputPathStr.endsWith(extension)) {
      throw new IllegalArgumentException(
          format("Input file '%s' is not a %s file.", inputPathStr, extension));
    }
  }
}
