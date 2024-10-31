package com.damflow.extractor.utils;

import soot.jimple.Stmt;
import soot.jimple.infoflow.cmdInfoflow;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ResultsManager {


    public static JsonObject createJsonResults(InfoflowResults results) {
        // Create a JSON Object to store the results
        JsonObject jsonResults = new JsonObject();

        // Store information inside the JSON Object
        jsonResults.add("sources", extractSourcesFromResults(results));
        jsonResults.add("sinks", extractSinksFromResults(results));
        jsonResults.add("pairs", extractPairsFromResults(results));
      
        
        return jsonResults;
    }


    public static JsonArray extractSourcesFromResults(InfoflowResults results) {
        Set<String> uniqueSources = new HashSet<>();
        JsonArray sourcesArray = new JsonArray();
    
        if (results != null && results.getResults() != null && !results.getResults().isEmpty()) {
            for (ResultSinkInfo sink : results.getResults().keySet()) {
                for (ResultSourceInfo source : results.getResults().get(sink)) {
                    uniqueSources.add(source.getStmt().getInvokeExpr().getMethod().toString());
                }
            }
        }
    
        for (String methodSignature : uniqueSources) {
            sourcesArray.add(methodSignature);
        }

        return sourcesArray;
    }


    public static JsonArray extractSinksFromResults(InfoflowResults results) {
        Set<String> uniqueSinks = new HashSet<>();
        JsonArray sinksArray = new JsonArray();

        if (results != null && results.getResults() != null && !results.getResults().isEmpty()) {
            for (ResultSinkInfo sink : results.getResults().keySet()) {
                uniqueSinks.add(sink.getStmt().getInvokeExpr().getMethod().toString());
            }
        }

        for (String methodSignature : uniqueSinks) {
            sinksArray.add(methodSignature);
        }

        return sinksArray;
    }


    public static JsonArray extractPairsFromResults(InfoflowResults results) {
        JsonArray sourcesAndSinksArray = new JsonArray();
        if (results != null && results.getResults() != null && !results.getResults().isEmpty()) {
            for (ResultSinkInfo sink : results.getResults().keySet()) {
                for (ResultSourceInfo source : results.getResults().get(sink)) {
                    JsonObject flowObject = new JsonObject();
                    flowObject.addProperty("source", source.getStmt().getInvokeExpr().getMethod().toString());
                    flowObject.addProperty("sink", sink.getStmt().getInvokeExpr().getMethod().toString());
                    sourcesAndSinksArray.add(flowObject);
                }
            }
        }
        return sourcesAndSinksArray;
    }



    public static void saveJsonObjectToFile(JsonObject jsonObject, String filePath) {
        // Determine the output path (replace .apk extension with .json)
        String outputPath = filePath.replace(".apk", ".json");
        System.out.println("\n💾 --- Saving Results --- 💾");

        // Check if all fields are empty arrays
        boolean allFieldsEmpty = true;
        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);
            if (element.isJsonArray() && !element.getAsJsonArray().isJsonNull() && element.getAsJsonArray().size() > 0) {
                allFieldsEmpty = false;
                break;
            }
        }

        // Print a warning message if all fields are empty arrays
        if (allFieldsEmpty) {
            System.out.println("\n--- ⚠️ No Data Flows --> Empty file.\n");
        }

        // Write results into a JSON file with proper indentation and without Unicode escaping
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            writer.write(gson.toJson(jsonObject));
            System.out.println("--- ✅ Results saved to file: " + outputPath);
        } catch (IOException e) {
            System.err.println("--- ⚠️ Error writing to file: " + e.getMessage());
        }
    }
  

    /**
     * Prints a summary of the results stored in a JsonObject.
     * 
     * Prints the count of sources, sinks, and pairs of sources and sinks extracted from the provided JsonObject.
     * 
     * @param jsonObject The JsonObject containing the summary of results.
     */
    public static void printJsonResultsSummary(JsonObject jsonObject){

        System.out.println("\n⚡ --- Results Summary --- ⚡");

        // Retrieve Data from the Json Object
        JsonArray sourcesArray = jsonObject.getAsJsonArray("sources");
        JsonArray sinksArray   = jsonObject.getAsJsonArray("sinks");
        JsonArray pairsArray   = jsonObject.getAsJsonArray("pairs");

        // Check if all fields are empty arrays
        boolean allFieldsEmpty = true;
        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);
            if (element.isJsonArray() && !element.getAsJsonArray().isJsonNull() && element.getAsJsonArray().size() > 0) {
                allFieldsEmpty = false;
                break;
            }
        }
        // Print a warning message
        if (allFieldsEmpty) {
            System.out.println("\n--- ⚠️ No Data Flows");
        }
        // Else Print the results
        else{
            System.out.println("--- ⭐ Sources Size        : " + sourcesArray.size());
            System.out.println("--- ⭐ Sinks Size          : " + sinksArray.size());
            System.out.println("--- ⭐ Pairs Size          : " + pairsArray.size());
        }

        return;
    }

}
