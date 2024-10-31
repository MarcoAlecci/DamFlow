package com.damflow.extractor.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandLineParser {

    // Options
    public String  ANDROID_PATH;
    public String  APK_PATH;
    public String  DIRECTION;
    public String  SOURCES_APPROACH;
   
    /**
     * Parses command line arguments and sets class variables accordingly.
     * 
     * Validates the provided arguments and sets class variables based on the parsed arguments.
     * Prints error messages for invalid arguments and starting message upon successful parsing.
     * 
     * @param args The command line arguments to be parsed.
     * @return true if the arguments are parsed successfully, false otherwise.
     */
    public boolean parseArguments(String[] args) {
        // Print Error Message
        if (args.length < 8 || !args[0].equals("-a") || !args[2].equals("-p") || !args[4].equals("-d") || !args[6].equals("-s")) {
            System.out.println("--- ⚠️ Invalid command line arguments.");
            System.out.println("Usage: -a <APK_PATH> -p <ANDROID_PATH> -d <forward|backward> -s <nosources|docflow>");
            return false;
        }

        // Print Starting Message
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(String.format("⚡ Starting DataFlowExtractor at %s ⚡\n", formattedDateTime));

        // Save command line options
        this.APK_PATH           = args[1];
        this.ANDROID_PATH       = args[3];
        this.DIRECTION          = args[5];
        this.SOURCES_APPROACH   = args[7];

        return true;
    }
}