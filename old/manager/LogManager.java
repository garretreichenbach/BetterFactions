package thederpgamer.betterfactions.manager;

import api.common.GameServer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.server.ServerMessage;
import thederpgamer.betterfactions.utils.DataUtils;
import thederpgamer.betterfactions.utils.DateUtils;
import thederpgamer.betterfactions.utils.NetworkUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * Manages mod logging and log files.
 *
 * @version 1.0 - [09/07/2021]
 * @author TheDerpGamer
 */
public class LogManager {

    private enum MessageType {
        DEBUG("[DEBUG]: "),
        INFO("[INFO]: "),
        WARNING("[WARNING]: "),
        ERROR("[ERROR]: "),
        CRITICAL("[CRITICAL]: "),
        FAILURE("[FAILURE]: ");

        public String prefix;

        MessageType(String prefix) {
            this.prefix = prefix;
        }
    }

    private static FileWriter logWriter;
    private static final LinkedList<String> messageQueue = new LinkedList<>();

    /**
     * Initializes the LogManager.
     */
    public static void initialize() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(!logsFolder.exists()) logsFolder.mkdirs();
        else {
            if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
                File[] logFiles = new File[logsFolder.listFiles().length];
                int j = logFiles.length - 1;
                for(int i = 0; i < logFiles.length && j >= 0; i ++) {
                    logFiles[i] = logsFolder.listFiles()[j];
                    j --;
                }

                for(File logFile : logFiles) {
                    String fileName = logFile.getName().replace(".txt", "");
                    int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
                    String newName = logFolderPath + "/log" + logNumber + ".txt";
                    if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
                    else logFile.delete();
                }
            }
        }
        try {
            File newLogFile = new File(logFolderPath + "/log0.txt");
            if(newLogFile.exists()) newLogFile.delete();
            newLogFile.createNewFile();
            logWriter = new FileWriter(newLogFile);
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Logs a debug message. Only outputs if debug-mode is enabled in config.
     * @param message The message to log
     */
    public static void logDebug(String message) {
        if(ConfigManager.getMainConfig().getBoolean("debug-mode")) logMessage(MessageType.DEBUG, message);
    }

    /**
     * Logs a general info message.
     * @param message The message to log
     */
    public static void logInfo(String message) {
        logMessage(MessageType.INFO, message);
    }

    /**
     * Logs a warning message, with a (nullable) exception that may have occurred to trigger it.
     * @param message A description of what was happening when the exception or other issue occurred
     * @param exception The exception that occurred (nullable)
     */
    public static void logWarning(String message, @Nullable Exception exception) {
        if(exception != null) logMessage(MessageType.WARNING, message + ":\n" + exception.getMessage());
        else logMessage(MessageType.WARNING, message);
    }

    /**
     * Logs an exception and prints the stack trace to the console.
     * @param message A description of what was happening when the exception occurred
     * @param exception The exception that occurred
     */
    public static void logException(String message, Exception exception) {
        exception.printStackTrace();
        logSilentException(message, exception);
    }

    /**
     * Logs an exception but does not print the stack trace to console.
     * @param message A description of what was happening when the exception occurred
     * @param exception The exception that occurred
     */
    public static void logSilentException(String message, Exception exception) {
        logMessage(MessageType.ERROR, message + ":\n" + exception.getMessage());
    }

    /**
     * Logs a critical exception, prints the stack trace to the console, then forces a system stop.
     * @param message A description of what was happening when the exception occurred
     * @param exception The exception that occurred
     */
    public static void logCritical(String message, Exception exception) {
        logMessage(MessageType.CRITICAL, message + ":\n" + exception.getMessage());
    }

    /**
     * Logs a failure to the console, meaning some sort of illegal action or state was detected. This doesn't necessarily mean an exception has occurred, rather that something
     * happened which will most likely lead to some sort of severe error or corruption later on if left unattended.
     * @param message A description of the failure
     * @param notifyAll Whether to notify all players on server or just an admin
     */
    public static void logFailure(String message, boolean notifyAll) {
        String startMessage = "A catastrophic failure has occurred\n";
        String endMessage = (notifyAll) ? "\nNotify an admin ASAP!" : "\nThis needs to be fixed ASAP!";
        logMessage(MessageType.FAILURE, startMessage + message + endMessage);
        if(NetworkUtils.onServer()) {
            ServerMessage serverMessage = new ServerMessage(new String[] {startMessage, message, endMessage}, ServerMessage.MESSAGE_TYPE_ERROR, !notifyAll);
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) playerState.sendServerMessage(serverMessage);
        }
    }

    /**
     * Logs a failure to the console, meaning some sort of illegal action or state was detected. This doesn't necessarily mean an exception has occurred, rather that something
     * happened which will most likely lead to some sort of severe error or corruption later on if left unattended.
     * <p>Hint: Use <code>Thread.currentThread().getStackTrace()[1].getLineNumber();</code> to get the line number being executed.</p>
     * @param message A description of the failure
     * @param notifyAll Whether to notify all players on server or just an admin
     * @param method The method in which the failure occurred
     * @param location The line number where the failure occurred
     */
    public static void logFailure(String message, boolean notifyAll, Method method, int location) {
        logFailure(message, notifyAll, method.getDeclaringClass().getName(), method.getName(), location);
    }

    /**
     * Logs a failure to the console, meaning some sort of illegal action or state was detected. This doesn't necessarily mean an exception has occurred, rather that something
     * happened which will most likely lead to some sort of severe error or corruption later on if left unattended.
     * <p>Hint: Use <code>Thread.currentThread().getStackTrace()[1].getLineNumber();</code> to get the line number being executed.</p>
     * @param message A description of the failure
     * @param notifyAll Whether to notify all players on server or just an admin
     * @param className The name of the class in which the failure occurred
     * @param methodName The name of the method in which the failure occurred
     * @param location The line number where the failure occurred
     */
    public static void logFailure(String message, boolean notifyAll, String className, String methodName, int location) {
        if(!methodName.endsWith("()")) methodName = methodName + "()";
        String sourceDesc = className + "." + methodName + " : " + location;
        String startMessage = "A catastrophic failure has occurred at " + sourceDesc + "\n";
        String endMessage = (notifyAll) ? "\nNotify an admin ASAP!" : "\nThis needs to be fixed ASAP!";
        logMessage(MessageType.FAILURE, startMessage + message + endMessage);
        if(NetworkUtils.onServer()) {
            ServerMessage serverMessage = new ServerMessage(new String[] {startMessage, message, endMessage}, ServerMessage.MESSAGE_TYPE_ERROR, !notifyAll);
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) playerState.sendServerMessage(serverMessage);
        }
    }

    private static void logMessage(MessageType messageType, String message) {
        if(!messageQueue.contains(message) || messageType.equals(MessageType.CRITICAL)) {
            String prefix = "[" + DateUtils.getTimeFormatted() + "] " + messageType.prefix;
            try {
                StringBuilder builder = new StringBuilder();
                builder.append(prefix);
                String[] lines = message.split("\n");
                if(lines.length > 1) {
                    for(int i = 0; i < lines.length; i ++) {
                        builder.append(lines[i]);
                        if(i < lines.length - 1) {
                            if(i > 1) for(int j = 0; j < prefix.length(); j ++) builder.append(" ");
                        }
                    }
                } else {
                    builder.append(message);
                }
                System.out.println(builder.toString());
                logWriter.append(builder.toString()).append("\n");
                logWriter.flush();
            } catch(IOException exception) {
                exception.printStackTrace();
            }
            if(messageType.equals(MessageType.CRITICAL)) System.exit(1);
        }
        if(messageQueue.size() == ConfigManager.getMainConfig().getInt("log-queue-size")) messageQueue.removeLast(); //Prevent spam from repeated messages
        messageQueue.addFirst(message);
    }

    /**
     * Clears all log files.
     */
    public static void clearLogs() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
            for(File logFile : logsFolder.listFiles()) {
                String logName = logFile.getName().replace(".txt", "");
                int logNumber = Integer.parseInt(logName.substring(logName.indexOf("log") + 3));
                if(logNumber != 0 && logNumber - 1 >= ConfigManager.getMainConfig().getInt("max-world-logs")) logFile.delete();
            }
        }
    }
}