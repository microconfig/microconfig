package deployment.mgmt;

import deployment.console.ConsoleApiExposer;
import deployment.mgmt.factory.MgmtFactory;

import static deployment.console.ConsoleApiExposerImpl.expose;
import static deployment.mgmt.update.restarter.RestarterImpl.UPDATE;
import static deployment.util.Logger.error;
import static deployment.util.PropertiesUtils.hasSystemFlag;

//todo2 ClassDataSharing. todo2 substrate vm
/*todo2 speedup start*/
public class MgmtMain {
    public static void main(String... args) {
        MgmtFactory mgmtFactory = new MgmtFactory();
        update(mgmtFactory, args);

        ConsoleApiExposer apiExposer = expose(mgmtFactory.getMgmt(), mgmtFactory.getMgmtJsonApi());
        apiExposer.invoke(args);
    }

    private static void update(MgmtFactory mgmtFactory, String[] args) {
        boolean skip = args.length == 0 || args.length == 1 && "status".equals(args[0]);
        if (skip) return;

        if (hasSystemFlag(UPDATE)) {
            mgmtFactory.newUpdateListener().onUpdate();
        }

        try {
            mgmtFactory.getMgmgUpdater().updateAndRestart(args);
        } catch (RuntimeException e) {
            error("Failed to update mgmt. Continuing using current version", e);
        }
    }
}