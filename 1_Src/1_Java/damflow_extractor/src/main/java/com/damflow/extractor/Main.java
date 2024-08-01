package com.damflow.extractor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import com.damflow.extractor.utils.CommandLineParser;



public class Main {
    public static void main(String[] args) {

        //// 1. Parse Command Line Options ////
        CommandLineParser cmdLineOptions = new CommandLineParser();
        if (!cmdLineOptions.parseArguments(args)) {
            System.exit(1); 
        }

    }
}