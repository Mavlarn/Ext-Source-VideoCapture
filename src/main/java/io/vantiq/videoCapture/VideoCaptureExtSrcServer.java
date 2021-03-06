package io.vantiq.videoCapture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzed.jne.ExtractException;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static io.vantiq.videoCapture.VideoCaptureExtConstants.*;

public class VideoCaptureExtSrcServer {
    public static final String LOCAL_CONFIG_FILE_NAME = "config.json";
    static final Logger log = LoggerFactory.getLogger(VideoCaptureExtSrcServer.class);

    public static void main(String[] argv) throws IOException, ExtractException {
        Options options = new Options();

        Option input = new Option("d", VANTIQ_HOME_DIR, true, "home directory for this source");
        input.setRequired(false);
        options.addOption(input);

        Option urlOpt = new Option("v", VANTIQ_URL, true, "VANTIQ server URL");
        urlOpt.setRequired(false);
        options.addOption(urlOpt);

        Option tokenOpt = new Option("t", VANTIQ_TOKEN, true, "VANTIQ access token");
        tokenOpt.setRequired(false);
        options.addOption(tokenOpt);

        Option sourceNameOpt = new Option("s", VANTIQ_SOURCE_NAME, true, "VANTIQ source name for this source");
        sourceNameOpt.setRequired(false);
        options.addOption(sourceNameOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, argv);
        } catch (ParseException e) {
            log.error("Error {} parsing command line with arguments: {}", e.getMessage(), argv);
            formatter.printHelp("<opc ua source server> <arguments>", options);
            System.exit(1);
        }


        Map<String, String> connectInfo = constructConfig(cmd);
        //OpcUaESClient.setDefaultStorageDirectory(homeDir);

       // RESTExtSource aSource = new RESTExtSource();

        //String sourceToUse = connectInfo.get(RESTExtSrcConstants.VANTIQ_SOURCENAME);

        //boolean itWorked = aSource.connectToVantiq(sourceToUse, connectInfo);

        VideoCaptureExtSrcSource extSrcSource = new VideoCaptureExtSrcSource();
        extSrcSource.connectToVantiq(connectInfo.get(VANTIQ_SOURCE_NAME), connectInfo);
        //if (aSource != null) {
            //aSource.close();
        //}

        System.exit(0);
    }

    public static Map<String, String> constructConfig(CommandLine cmd) {

        String homeDir = cmd.getOptionValue(VANTIQ_HOME_DIR);
        String vantiqUrl = cmd.getOptionValue(VANTIQ_URL);
        String token = cmd.getOptionValue(VANTIQ_TOKEN);
        String sourceName = cmd.getOptionValue(VANTIQ_SOURCE_NAME);

        if (homeDir == null) {
            log.info("Missing location of configuration information, using current location");
            homeDir = System.getProperty("user.dir");
        }

        Map<String, String> configMap = new HashMap<>();

        File locDir = new File(homeDir);
        if (!locDir.exists()) {
            log.error("Location specified for configuration directory ({}) does not exist.", locDir);
            return null;
        }

        String configFileName = locDir.getAbsolutePath() + File.separator + LOCAL_CONFIG_FILE_NAME;
        InputStream cfr = null;
        Map<String, Object> props = null;
        try {
            File configFile = new File(configFileName);
            if (configFile.exists()) {
                cfr = new FileInputStream(configFileName);
                ObjectMapper mapper = new ObjectMapper();
                props = mapper.readValue(configFile, Map.class);

                if (homeDir == null) {
                    homeDir = (String)props.get(VANTIQ_HOME_DIR);
                }
                if (vantiqUrl == null) {
                    vantiqUrl = (String)props.get(VANTIQ_URL);
                }
                if (token == null) {
                    token = (String)props.get(VANTIQ_TOKEN);
                }
                if (sourceName == null) {
                    sourceName = (String)props.get(VANTIQ_SOURCE_NAME);
                }
            }

            if (vantiqUrl == null || token == null || sourceName == null) {
                log.error("Invalid parameters.");
                return null;
            }
            // save into config map
            configMap.put(VANTIQ_URL, vantiqUrl);
            configMap.put(VANTIQ_TOKEN, token);
            configMap.put(VANTIQ_SOURCE_NAME, sourceName);
            configMap.put(VANTIQ_HOME_DIR, homeDir);

        } catch (IOException e) {
            log.error("Config file ({}) was not readable: {}", configFileName, e.getMessage());
            return null;
        } finally {
            if (cfr != null) {
                try {
                    cfr.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
        }
        log.debug("Extension Config: {}", configMap);
        return configMap;
    }
}
