package com.damflow.extractor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import com.damflow.extractor.utils.CommandLineParser;
import com.damflow.extractor.utils.Filter;
import com.damflow.extractor.utils.ResultsManager;
import com.damflow.extractor.utils.SourcesSinksManager;
import com.google.gson.JsonObject;
import com.jordansamhi.androspecter.FlowdroidUtils;
import com.jordansamhi.androspecter.SootUtils;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;



public class Main {
    public static void main(String[] args) {

        // 1. Parse Command Line Options
        CommandLineParser cmdLineOptions = new CommandLineParser();
        if (!cmdLineOptions.parseArguments(args)) {
            System.exit(1); 
        }

        // 2. Setup Soot 
        System.out.println("⚡ --- Using SooT --- ⚡");
        SootUtils su = new SootUtils();
        su.setupSoot(cmdLineOptions.ANDROID_PATH, cmdLineOptions.APK_PATH, true);

        // 3. Get the package name  
        FlowdroidUtils fu = new FlowdroidUtils(cmdLineOptions.APK_PATH);
        String pkgName = fu.getPackageName();
        System.out.println("--- 📦 Pkg Name: " + pkgName);

        // 4. Get Methods + Filtering 
        // Get ALl Methods
        Set<SootMethod> appMethods = su.getAllMethods();
        appMethods = Filter.filterNonSystemLibraries(appMethods);

        // 5. Loading Sources and sinks
        System.out.println("\n--- 🌊 Flow Analysis --- 🌊");
        System.out.println("--- 🔻 Number of Sources: " + SourcesSinksManager.sources.size());
        System.out.println("--- 🔺 Number of Sinks  : " + SourcesSinksManager.sinks.size());
        System.out.println("--- 🌊 Source & Sinks -->  " + cmdLineOptions.SOURCES_APPROACH);
        if ("docflow".equals(cmdLineOptions.SOURCES_APPROACH)) {
            SourcesSinksManager.loadSourcesAndSinks(("docflow"));
        }
        // Backward with only sinks
        else if ("nosources".equals(cmdLineOptions.SOURCES_APPROACH)) {
            SourcesSinksManager.loadSourcesAndSinksBackwardAnalysis(appMethods);
        } 
        // Error
        else {
            System.out.println("--- ⚠️  Warning: Use -docflow- or -nosources- approach.");
            System.exit(1);
        }
        System.out.println("--- 🔻 Number of Sources: " + SourcesSinksManager.sources.size());
        System.out.println("--- 🔺 Number of Sinks  : " + SourcesSinksManager.sinks.size());

         //// 6. Config Flowdroid //
        final InfoflowAndroidConfiguration ifac = new InfoflowAndroidConfiguration();
        ifac.getAnalysisFileConfig().setTargetAPKFile(cmdLineOptions.APK_PATH);
        ifac.getAnalysisFileConfig().setAndroidPlatformDir(cmdLineOptions.ANDROID_PATH);
        ifac.setMergeDexFiles(true);
        ifac.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        ifac.setCallgraphAlgorithm(InfoflowAndroidConfiguration.CallgraphAlgorithm.CHA); 
        System.out.println("\n--- 🌊 Algorithm -->  " + ifac.getCallgraphAlgorithm());


        // -- Direction
        if ("forward".equals(cmdLineOptions.DIRECTION)) {
            ifac.setDataFlowDirection(InfoflowAndroidConfiguration.DataFlowDirection.Forwards);
        } 
        else if ("backward".equals(cmdLineOptions.DIRECTION)) {
            ifac.setDataFlowDirection(InfoflowAndroidConfiguration.DataFlowDirection.Backwards);
        } 
        else {
            System.out.println("--- ⚠️  Warning: Use -forward- or -backward- direction.");
            System.exit(1);
        }
        System.out.println("\n--- 🌊 Direction -->  " + cmdLineOptions.DIRECTION);

        // To have full backward path reconstruction
        if("backward".equals(cmdLineOptions.DIRECTION)){
            ifac.setInspectSources(true);
            //ifac.setInspectSinks(true);
        }
        System.out.println("\n--- 🌊 Inspect Sources -->  " + ifac.getInspectSources());

        // To have full backward path reconstruction
        if("backward".equals(cmdLineOptions.DIRECTION)){
            ifac.getPathConfiguration().setPathBuildingAlgorithm(PathBuildingAlgorithm.ContextInsensitiveSourceFinder);
        }
        System.out.println("\n--- 🌊 Path Building Algorithm -->  " + ifac.getPathConfiguration().getPathBuildingAlgorithm());
        
         // -- Setup
        SetupApplication sa = new SetupApplication(ifac);

        // Taint Wrapper for Backward Analysis
        if("backward".equals(cmdLineOptions.DIRECTION)){

            System.out.println("\n--- 🌊 Taint Wrapper -->  Summary Taint Warapper \n" );
        
			final ITaintPropagationWrapper taintWrapper;
            SummaryTaintWrapper summaryTaintWrapper = null;

            LazySummaryProvider summaryProvider = null;
            try {
                summaryProvider = new LazySummaryProvider("summariesManual");
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            summaryTaintWrapper = new SummaryTaintWrapper(summaryProvider);
            taintWrapper = summaryTaintWrapper;
			sa.setTaintWrapper(taintWrapper);
		}

        // 7. Taint Analysis 
        long startTime = System.currentTimeMillis();
        System.out.println("\n⚡ --- Starting Taint Analysis (NO PATHS) --- ⚡");
   
        // Run It.
        InfoflowResults results = null;
		try {
            results = sa.runInfoflow(SourcesSinksManager.sources, SourcesSinksManager.sinks);}
        catch (Exception e) {
            System.out.println("--- ⚠️ Error RunInfoFlow:");
			System.out.println(e);
		}

        // Calculate elapsed time in milliseconds
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.printf("⚡ --- Finished Taint Analysis in %d seconds --- ⚡\n", Math.round(elapsedTime / 1000.0));


        // 8. Results 
        // Create a Json Object
        JsonObject jsonResults;
        jsonResults = ResultsManager.createJsonResults(results);

        // Print Results Summary
        ResultsManager.printJsonResultsSummary(jsonResults);

        // Save the results
        ResultsManager.saveJsonObjectToFile(jsonResults, cmdLineOptions.APK_PATH);

    }
}
