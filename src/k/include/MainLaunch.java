package k.include;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainLaunch {
    private static final FileNameExtensionFilter csrc = new FileNameExtensionFilter(
            "C/C++/Arduino Sources", "c", "cpp", "ino");
    private static final String INCLUDE_PREPROCESS_DIRECTIVESRC_REGEX = "\\Q#include \"\\E(.+)\\Q\"\\E",
            INCLUDE_PREPROCESS_DIRECTIVELIB_REGEX = "\\Q#include <\\E(.+)\\Q>\\E";
    private static final Pattern INCLUDE_PREPROCESS_DIRECTIVE_PATTERN = Pattern
            .compile(INCLUDE_PREPROCESS_DIRECTIVESRC_REGEX),
            INCLUDE_PREPROCESS_DIRECTIVELIB_PATTERN = Pattern
                    .compile(INCLUDE_PREPROCESS_DIRECTIVELIB_REGEX);

    public static void main(String[] args) throws FileNotFoundException,
            IOException {
        try {
            if (args.length < 1) {
                args = new String[1];
                args[0] = chooseFile();
            }
            if (args[0] == null || args[0].matches("^$")) {
                System.err.println("None, exiting");
                System.exit(0);
            }
            File f = new File(args[0]);
            if (f.isDirectory()) {
                throw new RuntimeException("Cannot use a directory");
            }
            if (!csrc.accept(f)) {
                throw new RuntimeException("Not " + csrc.getDescription());
            }
            String out = recursiveIncludes(f);
            FileOutputStream fos = new FileOutputStream(f.getAbsolutePath()
                    + ".txt");
            fos.write(out.getBytes());
            fos.close();
        } catch (StackOverflowError so) {
            System.err.println("stack max is " + so.getStackTrace().length);
        }
    }

    private static String recursiveIncludes(File f)
            throws FileNotFoundException, IOException {
        File dir = f.getParentFile();
        if (dir.isFile()) {
            System.err.println("File in file? We need to go deeper!");
            recursiveIncludes(dir);
        }
        File[] dirfiles = dir.listFiles();
        String filename = f.getName();
        String[] lines = readLines(new FileInputStream(f));
        String output = "";
        for (String line : lines) {
            Matcher m = INCLUDE_PREPROCESS_DIRECTIVE_PATTERN.matcher(line);
            if (m.matches()) {
                String toInclude = m.group(1);
                if (toInclude.endsWith(".h")) {
                    String no_h = toInclude.replace(".h", "");
                    if (no_h.equals(filename.substring(0,
                            filename.lastIndexOf('.')))) {
                        System.err.println("No need for header files! "
                                + filename + "=" + toInclude);
                        continue;
                    }
                    System.err.println("Matching header " + toInclude
                            + " with source...");
                    File src = null;
                    for (File in : dirfiles) {
                        System.err.println("Testing " + in.getAbsolutePath());
                        if (!toInclude.equals(in.getName())
                                && no_h.equals(in.getName().substring(0,
                                        in.getName().lastIndexOf('.')))) {
                            src = in;
                        }
                    }
                    System.err.println("Found " + src.getAbsolutePath());
                    toInclude = src.getName();
                    File include = new File(dir, toInclude);
                    line = recursiveIncludes(include);
                }
            }
            m = INCLUDE_PREPROCESS_DIRECTIVELIB_PATTERN.matcher(line);
            if (m.matches()) {
                continue;
            }
            output += line + "\n";
        }
        return output;
    }

    private static String[] readLines(InputStream stream) throws IOException {
        BufferedReader in = null;
        ArrayList<String> lines = new ArrayList<String>();
        try {
            in = new BufferedReader(new InputStreamReader(stream));
            for (String l = in.readLine(); l != null; l = in.readLine()) {
                lines.add(l);
            }
        } finally {
            if (in != null)
                in.close();
            stream.close();
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static String chooseFile() {
        JFileChooser temp = new JFileChooser();
        temp.setAcceptAllFileFilterUsed(false);
        temp.addChoosableFileFilter(csrc);
        if (temp.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return temp.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

}
