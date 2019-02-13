package deployment.mgmt.update.scriptgenerator;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Supplier;

import static io.microconfig.utils.FileUtils.userHome;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.FilePermissionUtils.writeExecutable;
import static io.microconfig.utils.OsUtil.isWindows;
import static mgmt.utils.ProcessUtil.currentJavaPath;

@RequiredArgsConstructor
public class MgmtScriptGeneratorImpl implements MgmtScriptGenerator {
    private final DeployFileStructure deployFileStructure;
    private final Autocomplete autocomplete;

    @Override
    public void generateMgmtScript() {
        File mgmtFile = mgmtFile();

        writeScript(mgmtFile);
        generateAutoComplete(mgmtFile);

        info("Generated mgmt script " + mgmtFile);
    }

    private void writeScript(File mgmtFile) {
        writeExecutable(mgmtFile, mgmtScript());
        addToPath(mgmtFile.getParentFile());
    }

    private void addToPath(File dir) {
        if (isWindows()) return;

        File bashrc = new File(userHome(), ".bashrc");
        String oldContent = readFully(bashrc);

        String path = "PATH=\"$PATH:" + dir.getAbsolutePath() + "\"";
        String delimiter = oldContent.contains(path) ? "" : "\n";
        String text = oldContent.replace(path, "") + delimiter + path;
        write(bashrc, text);
    }

    private void generateAutoComplete(File mgmtFile) {
        autocomplete.addAutoCompete(mgmtFile.getName());
    }

    private String mgmtScript() {
        Supplier<String> healthcheck = () ->
                "if [ \"$1\" = \"healthcheck\" ]; then\n"
                        + deployFileStructure.configs().getMgmtScriptsDir() + "/commands/healthcheck.sh ${@:2}\n"
                        + "  exit $?\n"
                        + "fi\n\n";

        Supplier<String> deletePostScript = () ->
                "post_mgmt_script='" + deployFileStructure.deploy().getPostMgmtScriptFile() + "'\n"
                        + "rm -f $post_mgmt_script\n\n";

        Supplier<String> mgmtRun = () ->
                currentJavaPath()
                        + " -Djava.security.egd=file:/dev/./urandom -XX:TieredStopAtLevel=1 -Xverify:none"
                        + " -jar " + deployFileStructure.deploy().getMgmtJarFile().getAbsolutePath()
                        + " $@\n\n";

        Supplier<String> executePostScript = () ->
                "if [ -f $post_mgmt_script ]; then\n"
                        + "  script=$(<$post_mgmt_script)\n"
                        + "  rm -rf $post_mgmt_script\n"
                        + "  $script\n"
                        + "fi";

        return isWindows() ? mgmtRun.get()
                : healthcheck.get() + deletePostScript.get() + mgmtRun.get() + executePostScript.get();
    }

    private File mgmtFile() {
        return deployFileStructure.deploy().getMgmtScriptFile();
    }
}