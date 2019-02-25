package deployment.mgmt.update.restarter;

import deployment.mgmt.factory.MgmtFactory;
import lombok.RequiredArgsConstructor;

import static io.microconfig.utils.Logger.info;

@RequiredArgsConstructor
public class UpdateListenerImpl implements UpdateListener {
    private final MgmtFactory mgmtFactory;

    @Override
    public void onUpdate() {
        info("Executing update listener");
        mgmtFactory.getMgmtScriptGenerator().generateMgmtScript();
    }
}