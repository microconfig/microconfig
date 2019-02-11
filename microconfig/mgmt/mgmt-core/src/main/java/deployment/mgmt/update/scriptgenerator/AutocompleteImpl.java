package deployment.mgmt.update.scriptgenerator;

import io.microconfig.configs.environment.ComponentGroup;
import deployment.mgmt.api.console.Mgmt;
import deployment.mgmt.api.json.MgmtJsonApi;
import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.update.updater.MgmtProperties;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.stream.Stream;

import static deployment.console.ConsoleApiExposerImpl.expose;
import static deployment.util.FileUtils.userHome;
import static deployment.util.FileUtils.write;
import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class AutocompleteImpl implements Autocomplete {
    private final ComponentGroupService componentGroupService;
    private final MgmtProperties mgmtProperties;

    @Override
    public void addAutoCompete(String mgmtScriptName) {
        String script = "" +
                "_mgmt () {\n" +
                "    if [[ \"$3\" == \"" + mgmtScriptName + "\" ]]\n" +
                "    then\n" +
                "         candidates=(" + mgmtCommands() + ")\n" +
                "    elif  [[ \"$3\" == \"ssh\" ]]\n" +
                "    then\n" +
                "         candidates=(" + componentGroups() + ")\n" +
                "     else\n" +
                "         candidates=(" + serviceNames() + ")\n" +
                "     fi \n" +

                "    COMPREPLY=()\n" +
                "    for candidate in ${candidates[@]}; do\n" +
                "        if [[ \"$candidate\" == \"$2\"* ]]; then\n" +
                "            COMPREPLY+=(\"$candidate\")\n" +
                "        fi\n" +
                "    done\n" +
                "}\n" +
                "complete -F _mgmt " + mgmtScriptName;

        write(new File(userHome(), ".bash_completion"), script);
    }

    private String mgmtCommands() {
        Stream<String> mgmt = expose(Mgmt.class, MgmtJsonApi.class).getCommandNames();
        Stream<String> scripts = of("healthcheck");

        return concat(mgmt, scripts)
                .distinct()
                .collect(joining(" "));
    }

    private String componentGroups() {
        try {
            return mgmtProperties.getEnvironmentProvider()
                    .getByName(componentGroupService.getEnv())
                    .getComponentGroups().stream()
                    .map(ComponentGroup::getName)
                    .collect(joining(" "));
        } catch (RuntimeException e) {
            return ""; //git is not cloned yet
        }
    }

    private String serviceNames() {
        return join(" ", componentGroupService.getServices());
    }
}
